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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stage 1 — 로스율의 원인 파악, 그리고 고정 간격 재시도로 로스율 제거.
 *
 * <p>{@code @EnableRetry} 를 적용한 상태에서 한 번만 돌린다. 수정 전 상태는 Stage 0 의 측정 결과가 그대로 대신한다. 출력 줄마다 mode 를 찍어
 * 재시도 어드바이저가 실제로 적용된 상태(RETRY_ON)에서 나온 로그인지 확인할 수 있게 한다.
 *
 * <ul>
 *   <li>S1 : 재시도 어드바이저가 프록시에 붙어 있는가, 트랜잭션보다 바깥인가 (재시도가 실제로 적용됐는가)
 *   <li>S2 : 같은 버전을 읽은 2건이 동시에 커밋하면 어떻게 되는가 (충돌이 확실한 조건에서 재시도가 흡수하는가)
 *   <li>S3 : Stage 0 과 같은 기준 부하(40건/초, 200건, 5회 반복)에서의 재고 로스율 (효과)
 * </ul>
 *
 * <p>구성: [A] 실행 환경 → [B] 입력 상수 → [C] 시도 집계·충돌 정렬 + 감사 보정 → [D] 시나리오 → [E] 실행기 → [F] 측정 헬퍼 → [G] 데이터
 * 헬퍼
 *
 * <p>환경변수: LOCK_REPEAT (S3 반복 횟수, 기본 5). 도착률·요청 수·시드는 Stage 0 과 같아야 하므로 상수로 고정한다.
 */
@Slf4j
@Testcontainers
@SpringBootTest
@Import(Stage1RetryTest.Config.class)
class Stage1RetryTest {

  // ═══ [A] 실행 환경 (Stage 0 과 동일) ═════════════════════════════════

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

  // ═══ [B] 입력 상수 (Stage 0 과 반드시 같은 값) ═══════════════════════

  private static final int RATE = 40; // 초당 도착 건수
  private static final int TOTAL = 200; // 한 번의 측정에서 보내는 요청 수
  private static final int INITIAL = 1000; // 시작 재고
  private static final long SEED = 42; // 도착 스케줄 시드 (Stage 0 과 같아야 같은 시각표를 받는다)
  private static final int REPEAT = envInt("LOCK_REPEAT", 5);

  private static int envInt(String k, int d) {
    String v = System.getenv(k);
    return v == null || v.isBlank() ? d : Integer.parseInt(v.trim());
  }

  @Autowired HubStockService hubStockService;
  @Autowired HubStockRepository hubStockRepository;
  @Autowired EntityManagerFactory emf;
  @Autowired AttemptListener listener;

  // ═══ [C] 시도 집계 + 충돌 정렬, 감사 보정 ═════════════════════════════

  /**
   * StockOrderEvent 는 order() 트랜잭션 안에서, 수량을 바꾼 직후(커밋 전)에 발행된다. 그래서
   *
   * <ul>
   *   <li>이벤트 수 = order() 시도 횟수 (재시도로 다시 실행된 시도도 센다)
   *   <li>S2 에서는 두 스레드를 이 지점에서 배리어로 세워 '둘 다 같은 버전을 읽고 수정한 상태'에서 함께 커밋하게 만든다. 충돌을 확률이 아니라 결정적으로 만드는
   *       장치이며, 배리어는 처음 도착한 parties 개에만 적용되어 재시도 시도는 그냥 통과한다.
   * </ul>
   */
  static class AttemptListener {
    private volatile CyclicBarrier barrier;
    private final AtomicInteger arrivals = new AtomicInteger();
    final AtomicInteger attempts = new AtomicInteger();

    void reset(int parties) {
      attempts.set(0);
      arrivals.set(0);
      barrier = parties > 1 ? new CyclicBarrier(parties) : null;
    }

    @EventListener
    public void on(StockOrderEvent e) {
      attempts.incrementAndGet();
      CyclicBarrier b = barrier;
      if (b != null && arrivals.incrementAndGet() <= b.getParties()) {
        try {
          b.await(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
          log.debug("[STAGE1] 충돌 정렬 실패(타임아웃/깨짐): {}", ex.getClass().getSimpleName());
        }
      }
    }
  }

  // 테스트에는 로그인 사용자가 없어 created_by / updated_by 가 null 로 들어가 NOT NULL 위반이 난다.
  // 모든 AuditorAware 를 감싸 '값이 없거나 예외면 고정 UUID' 로 대체한다 (Stage 0 과 동일).
  static final UUID TEST_AUDITOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @TestConfiguration
  static class Config {
    @Bean
    AttemptListener attemptListener() {
      return new AttemptListener();
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

  // ═══ [D] 시나리오 ═══════════════════════════════════════════════════

  // ── S1: 재시도 어드바이저가 붙어 있는가, 트랜잭션보다 바깥인가 ────────
  // order() 에는 @Retryable 이 붙어 있지만, 이 애노테이션은 재시도 어드바이저가 프록시에 등록되어야만
  // 동작한다. 등록 여부와 순서를 프록시의 어드바이저 목록으로 직접 확인한다.
  // 순서가 중요한 이유: 낙관적 락 예외는 order() 안이 아니라 '트랜잭션 커밋 시점'에 터지므로,
  // 재시도가 트랜잭션보다 바깥에 있어야 그 예외를 받을 수 있다.
  @Test
  void S1_재시도_어드바이저_존재와_순서() {
    if (!(hubStockService instanceof Advised advised)) {
      log.debug("[STAGE1][S1] HubStockService 는 프록시가 아님");
      return;
    }
    Advisor[] advisors = advised.getAdvisors();
    int retryIdx = -1;
    int txIdx = -1;
    for (int i = 0; i < advisors.length; i++) {
      String name = advisors[i].getClass().getName();
      Object ord = advisors[i] instanceof Ordered o ? o.getOrder() : "n/a";
      log.debug("[STAGE1][S1] advisor[{}] order={} class={}", i, ord, name);
      String lower = name.toLowerCase();
      if (retryIdx < 0 && lower.contains("retry")) retryIdx = i;
      if (txIdx < 0 && lower.contains("transaction")) txIdx = i;
    }
    String verdict;
    if (retryIdx < 0) {
      verdict = "RETRY_ADVISOR_ABSENT (재시도 어드바이저 없음 → @Retryable 이 동작하지 않는다)";
    } else if (txIdx < 0) {
      verdict = "TX_ADVISOR_ABSENT";
    } else if (retryIdx < txIdx) {
      verdict = "RETRY_OUTER (재시도가 트랜잭션을 감쌈 → 커밋 시점 예외도 재시도 대상)";
    } else {
      verdict = "TX_OUTER (트랜잭션이 재시도를 감쌈 → 커밋 시점 예외는 재시도 못 받음)";
    }
    log.debug(
        "[STAGE1][S1] mode={} retryIdx={} txIdx={} verdict={}", mode(), retryIdx, txIdx, verdict);
  }

  // ── S2: 같은 버전을 읽은 2건이 동시에 커밋 ─────────────────────────
  // 두 요청이 같은 재고 행을 같은 version 으로 읽고, 둘 다 수량을 줄인 상태에서 커밋한다.
  // 먼저 커밋한 쪽이 version 을 올리므로 나중 쪽의 UPDATE ... WHERE version=? 는 0행을 갱신하고,
  // 그 쪽이 낙관적 락 예외를 받는다 (손실의 직접 원인).
  // 재시도가 적용되어 있으면 실패한 쪽이 최신 버전으로 다시 시도해 성공해야 한다
  // → 실패 0, attempts 가 요청 수(2)보다 많음, 재고 2 감소
  @Test
  void S2_같은_버전에서_동시_커밋하는_2건() throws Exception {
    warmUp();
    HubStock seeded = seed(INITIAL);
    UUID key = seeded.getProductId();
    Statistics stats = statistics();
    stats.clear();
    listener.reset(2); // 처음 도착한 2건을 배리어로 세운다

    int n = 2;
    ExecutorService pool = Executors.newFixedThreadPool(n);
    CountDownLatch ready = new CountDownLatch(n);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<Res>> futures = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      futures.add(
          pool.submit(
              () -> {
                ready.countDown();
                go.await(); // 두 스레드가 준비될 때까지 기다렸다가 동시에 출발
                return orderTimed(key);
              }));
    }
    ready.await();
    long t0 = System.nanoTime();
    go.countDown();
    Outcome o = collect(futures, t0);
    pool.shutdown();

    int reflected = INITIAL - quantityOf(seeded);
    log.debug(
        "[STAGE1][S2] mode={} n={} 성공={} 실패={} attempts={} optimisticFailures={}"
            + " 재고반영={} 로스={} 전체처리={}ms 요청지연(전체)[{}] 요청지연(성공)[{}] errors={}",
        mode(),
        n,
        n - o.fail(),
        o.fail(),
        listener.attempts.get(),
        stats.getOptimisticFailureCount(),
        reflected,
        n - reflected,
        o.elapsedMs(),
        lat(o.latAll()),
        lat(o.latOk()),
        o.errors());

    assertThat(n - reflected).as("로스 = 실패한 요청 수").isEqualTo(o.fail());
    assertThat(o.fail()).as("재시도 적용 후에는 결정적 충돌 2건이 모두 성공").isZero();
  }

  // ── S3: 기준 부하에서의 재고 로스율 (Stage 0 과 같은 입력) ───────────
  // Stage 0 과 같은 시드·도착률·요청 수·반복 횟수. 달라진 것은 재시도 유무뿐이다.
  // 재시도 적용 후에도 남는 최종 실패와, 재시도 대기로 늘어난 지연을 함께 본다.
  @Test
  void S3_기준_부하_재고_로스율() throws Exception {
    warmUp();

    List<Double> lossRates = new ArrayList<>();
    List<Long> elapsed = new ArrayList<>();
    List<Long> allLat = new ArrayList<>();
    List<Long> okLat = new ArrayList<>();
    int lostSum = 0;
    int extraAttemptsSum = 0;

    for (int run = 0; run < REPEAT; run++) {
      Steady r = steady(run);
      double lossRate = 100.0 * r.lost() / TOTAL;
      lossRates.add(lossRate);
      elapsed.add(r.outcome().elapsedMs());
      allLat.addAll(r.outcome().latAll());
      okLat.addAll(r.outcome().latOk());
      lostSum += r.lost();
      extraAttemptsSum += r.attempts() - TOTAL;

      // 이 한 줄이 문서·스크린샷의 근거다. 재시도 = 총 시도 - 요청 수 (재시도로 추가 실행된 횟수)
      log.debug(
          "[STAGE1][S3] #{} mode={} rate={}/s total={} 재고반영={} 로스={} 로스율={}% 예외={}"
              + " attempts={} 재시도={} optimisticFailures={} 도착구간={}ms 전체처리={}ms"
              + " 요청지연(전체)[{}] 요청지연(성공)[{}] errors={}",
          run + 1,
          mode(),
          RATE,
          TOTAL,
          r.reflected(),
          r.lost(),
          String.format("%.1f", lossRate),
          r.outcome().fail(),
          r.attempts(),
          r.attempts() - TOTAL,
          r.optFailures(),
          r.arrivalSpanMs(),
          r.outcome().elapsedMs(),
          lat(r.outcome().latAll()),
          lat(r.outcome().latOk()),
          r.outcome().errors());

      // 측정 자체가 맞는지 검증: DB 기준 로스가 호출자가 받은 최종 실패 수와 같아야 한다.
      assertThat(r.lost()).as("로스 = 최종 실패한 요청 수").isEqualTo(r.outcome().fail());
    }

    log.debug(
        "[STAGE1][S3] SUMMARY mode={} runs={} rate={}/s total={}/run 로스합계={}/{} 로스율 avg={}% min={}% max={}%"
            + " 재시도합계={} 전체처리 avg={}ms 요청지연(전체)[{}] 요청지연(성공)[{}]",
        mode(),
        REPEAT,
        RATE,
        TOTAL,
        lostSum,
        REPEAT * TOTAL,
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).average().orElse(0)),
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).min().orElse(0)),
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).max().orElse(0)),
        extraAttemptsSum,
        Math.round(elapsed.stream().mapToLong(l -> l).average().orElse(0)),
        lat(allLat),
        lat(okLat));
  }

  // ═══ [E] 실행기 ═══════════════════════════════════════════════════

  private record Res(String err, long nanos) {}

  /** 요청 묶음의 결과. elapsedMs: 첫 요청 도착 ~ 마지막 요청 완료(전체 처리 시간). lat*: 요청 1건의 order() 소요(ns). */
  private record Outcome(
      int fail, long elapsedMs, List<Long> latAll, List<Long> latOk, Map<String, Integer> errors) {}

  /** S3 한 번의 측정 결과 (Stage 0 의 Result 에 attempts 가 더해진 것). */
  private record Steady(
      Outcome outcome,
      int reflected, // DB 기준으로 실제 재고에서 빠진 수량
      int lost, // TOTAL - reflected
      int attempts, // order() 총 시도 횟수 (재시도 포함)
      long optFailures, // 하이버네이트가 센 낙관적 락 실패 횟수 (재시도로 흡수된 것 포함)
      long arrivalSpanMs) {} // 첫 요청 도착 ~ 마지막 요청 도착

  /** 초당 RATE건의 푸아송 도착으로 TOTAL건을 보낸다. Stage 0 의 steady() 와 시각표 생성 방식이 같다. */
  private Steady steady(int run) throws Exception {
    HubStock seeded = seed(INITIAL);
    UUID key = seeded.getProductId();
    Statistics stats = statistics();
    stats.clear();
    listener.reset(0); // 배리어 없이 시도 횟수만 센다

    // 도착 시각표(ns): 지수분포 간격, 시드는 Stage 0 과 같은 SEED + RATE + run
    Random rnd = new Random(SEED + RATE + run);
    long[] arriveAt = new long[TOTAL];
    double t = 0;
    for (int i = 0; i < TOTAL; i++) {
      t += -Math.log(1 - rnd.nextDouble()) * 1_000_000_000.0 / RATE;
      arriveAt[i] = (long) t;
    }

    // 요청마다 별도 스레드. 재시도로 한 요청이 500ms 씩 오래 걸려도 다음 요청이 풀 크기 때문에 줄 서지 않도록 cached 풀
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
        listener.attempts.get(),
        stats.getOptimisticFailureCount(),
        TimeUnit.NANOSECONDS.toMillis(arriveAt[TOTAL - 1]));
  }

  private Outcome collect(List<Future<Res>> futures, long t0) throws Exception {
    int fail = 0;
    Map<String, Integer> errors = new TreeMap<>();
    List<Long> all = new ArrayList<>();
    List<Long> ok = new ArrayList<>();
    for (Future<Res> f : futures) {
      Res res = f.get(120, TimeUnit.SECONDS);
      all.add(res.nanos());
      if (res.err() == null) {
        ok.add(res.nanos());
      } else {
        fail++;
        errors.merge(res.err(), 1, Integer::sum);
      }
    }
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
    return new Outcome(fail, elapsedMs, all, ok, errors);
  }

  // ═══ [F] 측정 헬퍼 ════════════════════════════════════════════════

  private boolean retryActive() {
    if (!(hubStockService instanceof Advised advised)) return false;
    return Arrays.stream(advised.getAdvisors())
        .anyMatch(a -> a.getClass().getName().toLowerCase().contains("retry"));
  }

  private String mode() {
    return retryActive() ? "RETRY_ON" : "RETRY_OFF";
  }

  private Statistics statistics() {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }

  /** order() 한 번과 그 소요 시간(재시도·backoff 대기 포함). 성공이면 err 는 null. */
  private Res orderTimed(UUID key) {
    long s = System.nanoTime();
    String err = null;
    try {
      hubStockService.order(request(key), null);
    } catch (Throwable t) {
      err = t.getClass().getSimpleName();
    }
    return new Res(err, System.nanoTime() - s);
  }

  /** avg / p50 / p95 / max (ms). 가장 가까운 순위(nearest-rank) 방식. */
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
    listener.reset(0);
  }

  private int quantityOf(HubStock seeded) {
    return hubStockRepository.findById(seeded.getId()).orElseThrow().getQuantity();
  }

  // ═══ [G] 데이터 헬퍼 (Stage 0 과 동일) ════════════════════════════════

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
