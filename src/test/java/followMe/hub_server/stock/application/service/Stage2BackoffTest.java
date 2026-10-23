package followMe.hub_server.stock.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
import followMe.hub_server.stock.domain.event.StockOrderEvent;
import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import followMe.hub_server.stock.domain.service.PermissionChecker;
import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stage 2 — 고정 500ms 재시도의 재충돌을 랜덤 backoff 로 줄인다.
 *
 * <p>Stage 1 과 같은 입력(40건/초, 200건, 시드 42, 5회)으로 한 번만 돌린다. 바뀐 것은 {@code @Backoff} 하나뿐이고, Stage 1 의 측정
 * 결과가 비교 기준이다. Stage 1 에는 없던 두 가지를 추가로 기록한다.
 *
 * <ul>
 *   <li>요청별 시도 횟수의 분포 (1회에 성공한 요청, 2회째에 성공한 요청, ... , 5회를 다 쓴 요청)
 *   <li>같은 요청의 시도와 시도 사이 간격 (backoff 가 실제로 고정인지 랜덤인지 로그로 확인)
 * </ul>
 *
 * <p>구성: [A] 실행 환경 → [B] 입력 상수 → [C] 요청별 시도 기록 + 감사 보정 → [D] 시나리오 → [E] 실행기 → [F] 측정 헬퍼 → [G] 데이터
 * 헬퍼
 *
 * <p>환경변수: LOCK_REPEAT (반복 횟수, 기본 5). 도착률·요청 수·시드는 Stage 0·1 과 같아야 하므로 상수로 고정한다.
 */
@Slf4j
@Testcontainers
@SpringBootTest
@Import(Stage2BackoffTest.Config.class)
class Stage2BackoffTest {

  // ═══ [A] 실행 환경 (Stage 0·1 과 동일) ═════════════════════════════

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    r.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    r.add("spring.cloud.config.enabled", () -> "false");
    r.add("spring.cloud.config.import-check.enabled", () -> "false");
    r.add("eureka.client.enabled", () -> "false");
    r.add("spring.kafka.listener.auto-startup", () -> "false");
    r.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
    r.add(
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener",
        () -> "WARN");
  }

  // ═══ [B] 입력 상수 (Stage 0·1 과 반드시 같은 값) ═════════════════════

  private static final int RATE = 40; // 초당 도착 건수
  private static final int TOTAL = 200; // 한 번의 측정에서 보내는 요청 수
  private static final int INITIAL = 1000; // 시작 재고
  private static final long SEED = 42; // 도착 스케줄 시드 (Stage 0·1 과 같아야 같은 시각표를 받는다)
  private static final int REPEAT = envInt("LOCK_REPEAT", 5);

  private static int envInt(String k, int d) {
    String v = System.getenv(k);
    return v == null || v.isBlank() ? d : Integer.parseInt(v.trim());
  }

  @Autowired HubStockService hubStockService;
  @Autowired HubStockRepository hubStockRepository;
  @Autowired EntityManagerFactory emf;
  @Autowired AttemptRecorder recorder;

  // ═══ [C] 요청별 시도 기록, 감사 보정 ═════════════════════════════════

  /**
   * StockOrderEvent 는 order() 트랜잭션 안에서, 수량을 바꾼 직후(커밋 전)에 발행된다. 그래서 이벤트가 올 때마다 시각을 적어 두면 그 요청의 시도
   * 횟수와 시도 사이 간격을 알 수 있다. 재시도는 호출한 스레드 안에서 일어나므로 스레드별(ThreadLocal)로 기록하면 요청마다 구분된다.
   */
  static class AttemptRecorder {
    private final ThreadLocal<List<Long>> times = ThreadLocal.withInitial(ArrayList::new);

    void begin() {
      times.get().clear(); // 스레드 풀의 스레드는 재사용되므로 요청 시작 때마다 비운다
    }

    List<Long> snapshot() {
      return new ArrayList<>(times.get());
    }

    @EventListener
    public void on(StockOrderEvent e) {
      times.get().add(System.nanoTime());
    }
  }

  // 테스트에는 로그인 사용자가 없어 created_by / updated_by 가 null 로 들어가 NOT NULL 위반이 난다.
  // 모든 AuditorAware 를 감싸 '값이 없거나 예외면 고정 UUID' 로 대체한다 (Stage 0·1 과 동일).
  static final UUID TEST_AUDITOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @TestConfiguration
  static class Config {
    @Bean
    AttemptRecorder attemptRecorder() {
      return new AttemptRecorder();
    }

    @Bean // static 이어야 BeanPostProcessor 가 다른 빈보다 먼저 만들어진다
    static BeanPostProcessor auditorFallback() {
      return new BeanPostProcessor() {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          if (bean instanceof AuditorAware delegate) {
            return (AuditorAware<UUID>)
                () -> {
                  try {
                    Optional<?> current = delegate.getCurrentAuditor();
                    if (current.isPresent()) return (Optional<UUID>) current;
                  } catch (RuntimeException ignored) {
                    // 인증 컨텍스트 없음 → 고정값 사용
                  }
                  return Optional.of(TEST_AUDITOR);
                };
          }
          return bean;
        }
      };
    }
  }

  // ═══ [D] 시나리오 ═════════════════════════════════════════════════════

  // ── S1: 기준 부하에서의 재고 로스율·시도 분포·시도 간격 ──────────────
  // Stage 1 과 같은 시드·도착률·요청 수·반복 횟수. 달라진 것은 backoff 설정뿐이다.
  // 로스가 0 으로 유지되는지, 여러 번 재시도하는 요청이 줄었는지, 지연이 어떻게 달라졌는지를 본다.
  @Test
  void S1_기준_부하_재고_로스율과_시도_분포() throws Exception {
    warmUp();

    List<Double> lossRates = new ArrayList<>();
    List<Long> elapsed = new ArrayList<>();
    List<Long> allLat = new ArrayList<>();
    List<Long> allGaps = new ArrayList<>();
    Map<Integer, Integer> totalDist = new TreeMap<>();
    int lostSum = 0;
    int extraAttemptsSum = 0;

    for (int run = 0; run < REPEAT; run++) {
      Steady r = steady(run);
      Outcome o = r.outcome();
      double lossRate = 100.0 * r.lost() / TOTAL;
      lossRates.add(lossRate);
      elapsed.add(o.elapsedMs());
      allLat.addAll(o.latAll());
      allGaps.addAll(o.gaps());
      o.attemptDist().forEach((k, v) -> totalDist.merge(k, v, Integer::sum));
      lostSum += r.lost();
      extraAttemptsSum += o.attempts() - TOTAL;

      // 이 한 줄이 문서·스크린샷의 근거다. 재시도 = 총 시도 - 요청 수
      log.debug(
          "[STAGE2][S1] #{} rate={}/s total={} 재고반영={} 로스={} 로스율={}% 예외={}"
              + " attempts={} 재시도={} optimisticFailures={} 도착구간={}ms 전체처리={}ms"
              + " 요청지연(전체)[{}] 시도분포={} 시도간격[{}] errors={}",
          run + 1,
          RATE,
          TOTAL,
          r.reflected(),
          r.lost(),
          String.format("%.1f", lossRate),
          o.fail(),
          o.attempts(),
          o.attempts() - TOTAL,
          r.optFailures(),
          r.arrivalSpanMs(),
          o.elapsedMs(),
          lat(o.latAll()),
          o.attemptDist(),
          lat(o.gaps()),
          o.errors());

      // 측정 자체가 맞는지 검증: DB 기준 로스가 호출자가 받은 최종 실패 수와 같아야 한다.
      assertThat(r.lost()).as("로스 = 최종 실패한 요청 수").isEqualTo(o.fail());
    }

    log.debug(
        "[STAGE2][S1] SUMMARY runs={} rate={}/s total={}/run 로스합계={}/{} 로스율 avg={}% min={}% max={}%"
            + " 재시도합계={} 시도분포={} 시도간격[{}] 전체처리 avg={}ms 요청지연(전체)[{}]",
        REPEAT,
        RATE,
        TOTAL,
        lostSum,
        REPEAT * TOTAL,
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).average().orElse(0)),
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).min().orElse(0)),
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).max().orElse(0)),
        extraAttemptsSum,
        totalDist,
        lat(allGaps),
        Math.round(elapsed.stream().mapToLong(l -> l).average().orElse(0)),
        lat(allLat));
  }

  // ═══ [E] 실행기 ═══════════════════════════════════════════════════════

  /** 요청 1건의 결과. attempts: 이 요청의 order() 시도 횟수, gaps: 시도와 시도 사이 간격(ns). */
  private record Res(String err, long nanos, int attempts, List<Long> gaps) {}

  /**
   * 요청 묶음의 결과. elapsedMs: 첫 요청 도착 ~ 마지막 요청 완료(전체 처리 시간). attemptDist: 시도 횟수별 요청 수. gaps: 모든 요청의 시도
   * 간격(ns).
   */
  private record Outcome(
      int fail,
      long elapsedMs,
      int attempts,
      List<Long> latAll,
      List<Long> gaps,
      Map<Integer, Integer> attemptDist,
      Map<String, Integer> errors) {}

  /** 한 번의 측정 결과. */
  private record Steady(
      Outcome outcome,
      int reflected, // DB 기준으로 실제 재고에서 빠진 수량
      int lost, // TOTAL - reflected
      long optFailures, // 하이버네이트가 센 낙관적 락 실패 횟수 (재시도로 흡수된 것 포함)
      long arrivalSpanMs) {} // 첫 요청 도착 ~ 마지막 요청 도착

  /** 초당 RATE건의 푸아송 도착으로 TOTAL건을 보낸다. Stage 0·1 의 steady() 와 시각표 생성 방식이 같다. */
  private Steady steady(int run) throws Exception {
    HubStock seeded = seed(INITIAL);
    UUID key = seeded.getProductId();
    Statistics stats = statistics();
    stats.clear();

    // 도착 시각표(ns): 지수분포 간격, 시드는 Stage 0·1 과 같은 SEED + RATE + run
    Random rnd = new Random(SEED + RATE + run);
    long[] arriveAt = new long[TOTAL];
    double t = 0;
    for (int i = 0; i < TOTAL; i++) {
      t += -Math.log(1 - rnd.nextDouble()) * 1_000_000_000.0 / RATE;
      arriveAt[i] = (long) t;
    }

    // 요청마다 별도 스레드. 재시도로 한 요청이 오래 걸려도 다음 요청이 풀 크기 때문에 줄 서지 않도록 cached 풀
    ExecutorService pool = Executors.newCachedThreadPool();
    List<Future<Res>> futures = new ArrayList<>();
    long t0 = System.nanoTime();
    for (int i = 0; i < TOTAL; i++) {
      long wait = t0 + arriveAt[i] - System.nanoTime();
      if (wait > 0) LockSupport.parkNanos(wait);
      futures.add(pool.submit(() -> orderTimed(key)));
    }
    Outcome o = collect(futures, t0);
    pool.shutdown();

    int reflected = INITIAL - quantityOf(seeded);
    return new Steady(
        o,
        reflected,
        TOTAL - reflected,
        stats.getOptimisticFailureCount(),
        TimeUnit.NANOSECONDS.toMillis(arriveAt[TOTAL - 1]));
  }

  private Outcome collect(List<Future<Res>> futures, long t0) throws Exception {
    int fail = 0;
    int attempts = 0;
    Map<String, Integer> errors = new TreeMap<>();
    Map<Integer, Integer> dist = new TreeMap<>();
    List<Long> all = new ArrayList<>();
    List<Long> gaps = new ArrayList<>();
    for (Future<Res> f : futures) {
      Res res = f.get(120, TimeUnit.SECONDS);
      all.add(res.nanos());
      gaps.addAll(res.gaps());
      attempts += res.attempts();
      dist.merge(res.attempts(), 1, Integer::sum);
      if (res.err() != null) {
        fail++;
        errors.merge(res.err(), 1, Integer::sum);
      }
    }
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
    return new Outcome(fail, elapsedMs, attempts, all, gaps, dist, errors);
  }

  // ═══ [F] 측정 헬퍼 ═════════════════════════════════════════════════════

  private Statistics statistics() {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }

  /** order() 한 번과 그 소요 시간(재시도·backoff 대기 포함), 시도 횟수, 시도 간격. 성공이면 err 는 null. */
  private Res orderTimed(UUID key) {
    recorder.begin();
    long s = System.nanoTime();
    String err = null;
    try {
      hubStockService.order(request(key), null);
    } catch (Throwable t) {
      err = t.getClass().getSimpleName();
    }
    long nanos = System.nanoTime() - s;
    List<Long> times = recorder.snapshot();
    List<Long> gaps = new ArrayList<>();
    for (int i = 1; i < times.size(); i++) {
      gaps.add(times.get(i) - times.get(i - 1));
    }
    return new Res(err, nanos, times.size(), gaps);
  }

  /** avg / p50 / p95 / max (ms). 가장 가까운 순위(nearest-rank) 방식. 비어 있으면 "-". */
  private static String lat(List<Long> nanos) {
    if (nanos.isEmpty()) return "-";
    long[] a = nanos.stream().mapToLong(Long::longValue).sorted().toArray();
    double avg = Arrays.stream(a).average().orElse(0) / 1e6;
    return String.format(
        "avg=%.1f p50=%.1f p95=%.1f max=%.1f ms",
        avg, pct(a, 0.50), pct(a, 0.95), a[a.length - 1] / 1e6);
  }

  private static double pct(long[] sorted, double p) {
    int idx = (int) Math.ceil(p * sorted.length) - 1;
    return sorted[Math.max(idx, 0)] / 1e6;
  }

  /** 순차로 20번 호출해 JIT / 커넥션 풀 / Hibernate 초기화 비용을 미리 치른다 (순차라 충돌 없음). */
  private void warmUp() {
    UUID key = seed(INITIAL).getProductId();
    for (int i = 0; i < 20; i++) {
      hubStockService.order(request(key), null);
    }
    statistics().clear();
  }

  private int quantityOf(HubStock seeded) {
    return hubStockRepository.findById(seeded.getId()).orElseThrow().getQuantity();
  }

  // ═══ [G] 데이터 헬퍼 (Stage 0·1 과 동일) ═════════════════════════════

  private HubStock seed(int quantity) {
    UUID productId = UUID.randomUUID();
    HubStock stock =
        HubStock.create(
            UUID.randomUUID(), // requesterId
            productId,
            "CODE-" + productId.toString().substring(0, 8),
            "상품-" + productId.toString().substring(0, 8),
            UUID.randomUUID(), // hubId
            UUID.randomUUID(), // vendorId
            "업체",
            quantity,
            permissive(PermissionChecker.class),
            permissive(HubExistenceChecker.class),
            permissive(VendorProductExistenceChecker.class));
    return hubStockRepository.saveAndFlush(stock);
  }

  /** HubStock.create 가 묻는 권한·허브·업체상품 검증은 이번 측정과 무관하므로 boolean 은 전부 true. */
  private static <T> T permissive(Class<T> type) {
    return Mockito.mock(
        type,
        inv -> {
          Class<?> rt = inv.getMethod().getReturnType();
          if (rt == boolean.class || rt == Boolean.class) return true;
          return Mockito.RETURNS_DEFAULTS.answer(inv);
        });
  }

  // 주문 요청 1건: 같은 상품을 1개 차감. orderId 는 매번 새로 만든다.
  private OrderHubStockRequest request(UUID productId) {
    return OrderHubStockRequest.builder()
        .orderId(UUID.randomUUID())
        .products(List.of(OrderHubStockRequest.Product.builder().id(productId).quantity(1).build()))
        .build();
  }
}
