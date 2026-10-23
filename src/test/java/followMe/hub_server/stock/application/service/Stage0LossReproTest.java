package followMe.hub_server.stock.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
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
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stage 0 — 문제 시각화: 재고 로스율 재현.
 *
 * <p>같은 상품에 초당 40건(푸아송 도착)으로 주문 요청 200건이 들어올 때, 요청 중 몇 %가 재고에 반영되지 않는지를 DB 기준으로 잰다. 같은 측정을 5회 반복해
 * 평균과 범위를 낸다. 코드는 고치지 않고 현재 상태(재시도 비활성) 그대로 측정한다.
 *
 * <p>구성: [A] 실행 환경 → [B] 입력 상수 → [C] 감사 컬럼 보정 → [D] 시나리오 S1 → [E] 실행기 → [F] 측정 헬퍼 → [G] 데이터 헬퍼
 *
 * <p>환경변수: LOCK_REPEAT (반복 횟수, 기본 5). 도착률·요청 수·시드는 Stage 1·2 와 같아야 하므로 상수로 고정한다.
 */
@Slf4j
@Testcontainers
@SpringBootTest
@Import(Stage0LossReproTest.AuditorConfig.class)
class Stage0LossReproTest {

  // ═══ [A] 실행 환경 ═══════════════════════════════════════════════

  // 운영과 같은 DB 엔진(Postgres 15). 낙관적 락 충돌은 "UPDATE ... WHERE version = ?" 가
  // 0행을 갱신했을 때 감지되므로 인메모리 대체물이 아닌 진짜 DB 위에서 재현해야 한다.
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    // DB 접속 → 방금 띄운 컨테이너 (접속 정보는 기동 후에야 알 수 있어 테스트 시점에 주입)
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    // 테이블은 엔티티에서 자동 생성, 컨텍스트가 닫히면 삭제
    r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    // 하이버네이트가 센 "낙관적 락 실패 횟수"를 두 번째 증거로 쓴다
    r.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    // 측정과 무관한 외부 의존 차단: config 서버, Eureka, Kafka 리스너
    // (이번 측정은 OrderEventHandler 가 아니라 HubStockService.order() 를 직접 호출한다)
    r.add("spring.cloud.config.enabled", () -> "false");
    r.add("spring.cloud.config.import-check.enabled", () -> "false");
    r.add("eureka.client.enabled", () -> "false");
    r.add("spring.kafka.listener.auto-startup", () -> "false");
    r.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
    // 통계 로거의 세션별 요약 로그가 결과 로그를 가리지 않게 WARN 으로 올린다
    r.add(
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener",
        () -> "WARN");
  }

  // ═══ [B] 입력 상수 (Stage 1·2 와 반드시 같은 값) ═══════════════════

  private static final int RATE = 40; // 초당 도착 건수
  private static final int TOTAL = 200; // 한 번의 측정에서 보내는 요청 수
  private static final int INITIAL = 1000; // 시작 재고. 요청 총량보다 커서 '재고 부족'이 로스 원인이 되지 않는다
  private static final long SEED = 42; // 도착 스케줄 시드
  private static final int REPEAT = envInt("LOCK_REPEAT", 5); // 반복 횟수

  private static int envInt(String k, int d) {
    String v = System.getenv(k);
    return v == null || v.isBlank() ? d : Integer.parseInt(v.trim());
  }

  @Autowired HubStockService hubStockService; // 측정 대상 (수정하지 않은 실제 빈)
  @Autowired HubStockRepository hubStockRepository; // 시드 저장 + 최종 재고 조회
  @Autowired EntityManagerFactory emf; // 하이버네이트 통계 입구

  // ═══ [C] 감사(Audit) 컬럼 보정 ═══════════════════════════════════

  // 엔티티는 created_by / updated_by 를 자동으로 채우는데 테스트에는 로그인 사용자가 없어
  // NOT NULL 위반이 난다. 모든 AuditorAware 를 감싸 '값이 없거나 예외면 고정 UUID' 로 대체한다.
  static final UUID TEST_AUDITOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @TestConfiguration
  static class AuditorConfig {
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

  // ═══ [D] 시나리오 S1 — 기준 부하(40건/초)에서의 재고 로스율 ═════════

  @Test
  void S1_기준_부하_재고_로스율() throws Exception {
    warmUp(); // 첫 측정에 JIT / 커넥션 풀 / 하이버네이트 초기화 비용이 섞이지 않게 먼저 데운다

    List<Double> lossRates = new ArrayList<>();
    List<Long> elapsed = new ArrayList<>();
    List<Long> allLat = new ArrayList<>(); // 반복 전체를 합친 요청별 지연
    List<Long> okLat = new ArrayList<>();
    int lostSum = 0;

    for (int run = 0; run < REPEAT; run++) {
      Result r = steady(run);
      double lossRate = 100.0 * r.lost() / TOTAL;
      lossRates.add(lossRate);
      elapsed.add(r.elapsedMs());
      allLat.addAll(r.latAll());
      okLat.addAll(r.latOk());
      lostSum += r.lost();

      // 이 한 줄이 문서·스크린샷의 근거다. 값은 전부 측정된 것만 찍는다.
      log.debug(
          "[STAGE0][S1] #{} rate={}/s total={} 재고반영={} 로스={} 로스율={}% 예외={} optimisticFailures={}"
              + " 도착구간={}ms 전체처리={}ms 요청지연(전체)[{}] 요청지연(성공)[{}] errors={}",
          run + 1,
          RATE,
          TOTAL,
          r.reflected(),
          r.lost(),
          String.format("%.1f", lossRate),
          r.fail(),
          r.optFailures(),
          r.arrivalSpanMs(),
          r.elapsedMs(),
          lat(r.latAll()),
          lat(r.latOk()),
          r.errors());

      // 측정 자체가 맞는지 검증: DB 기준 로스가 호출자가 받은 실패 수와 같아야 한다.
      // 어긋나면 "예외 없이 조용히 사라진 요청"이 있다는 뜻이다.
      assertThat(r.lost()).as("로스 = 실패한 요청 수").isEqualTo(r.fail());
    }

    log.debug(
        "[STAGE0][S1] SUMMARY runs={} rate={}/s total={}/run 로스합계={}/{} 로스율 avg={}% min={}% max={}%"
            + " 전체처리 avg={}ms 요청지연(전체)[{}] 요청지연(성공)[{}]",
        REPEAT,
        RATE,
        TOTAL,
        lostSum,
        REPEAT * TOTAL,
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).average().orElse(0)),
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).min().orElse(0)),
        String.format("%.1f", lossRates.stream().mapToDouble(d -> d).max().orElse(0)),
        Math.round(elapsed.stream().mapToLong(l -> l).average().orElse(0)),
        lat(allLat),
        lat(okLat));
  }

  // ═══ [E] 실행기 ═══════════════════════════════════════════════════

  private record Result(
      int fail, // 호출자가 예외를 받은 요청 수
      int reflected, // DB 기준으로 실제 재고에서 빠진 수량 (= 반영된 요청 수)
      int lost, // TOTAL - reflected (재고에 반영되지 않은 요청 수)
      long optFailures, // 하이버네이트가 센 낙관적 락 실패 횟수
      long arrivalSpanMs, // 첫 요청 도착 ~ 마지막 요청 도착 (입력 스케줄 길이)
      long elapsedMs, // 첫 요청 도착 ~ 마지막 요청 완료 (전체 처리 시간)
      List<Long> latAll, // 요청 1건의 order() 소요(ns): 전체 요청
      List<Long> latOk, // 요청 1건의 order() 소요(ns): 성공한 요청만
      Map<String, Integer> errors) {} // 예외 클래스명별 건수

  // 요청 1건의 결과: 예외 클래스명(성공이면 null)과 order() 소요 시간(ns)
  private record Res(String err, long nanos) {}

  /** 초당 RATE건의 푸아송 도착으로 TOTAL건을 보내고 결과를 DB 로 확인한다. */
  private Result steady(int run) throws Exception {
    // (1) 측정마다 새 재고(1000개)를 만든다. 앞 측정의 잔량이 섞이지 않는다.
    HubStock seeded = seed(INITIAL);
    UUID key = seeded.getProductId();
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();

    // (2) 도착 시각표를 먼저 다 만든다 (ns). 간격을 지수분포로 뽑으면 "평균 초당 RATE건, 때로는
    //     몰리고 때로는 뜸한" 푸아송 도착이 된다. 시드가 고정이라 Stage 1·2 도 같은 시각표를 받는다.
    Random rnd = new Random(SEED + RATE + run);
    long[] arriveAt = new long[TOTAL];
    double t = 0;
    for (int i = 0; i < TOTAL; i++) {
      t += -Math.log(1 - rnd.nextDouble()) * 1_000_000_000.0 / RATE;
      arriveAt[i] = (long) t;
    }

    // (3) 시각표대로 요청을 발사한다. 요청마다 별도 스레드에서 order() 를 부르므로 앞 요청이
    //     처리 중일 때 다음 요청이 도착하면 실제로 겹친다 (충돌의 원인). 스레드 수를 고정하면
    //     요청이 줄을 서서 겹침이 인위적으로 줄 수 있어 cached 풀을 쓴다.
    ExecutorService pool = Executors.newCachedThreadPool();
    List<Future<Res>> futures = new ArrayList<>();
    long t0 = System.nanoTime();
    for (int i = 0; i < TOTAL; i++) {
      long wait = t0 + arriveAt[i] - System.nanoTime();
      if (wait > 0) LockSupport.parkNanos(wait); // sleep 은 ms 단위라 도착 간격이 뭉개진다
      futures.add(pool.submit(() -> orderTimed(key)));
    }

    // (4) 모든 요청이 끝날 때까지 결과를 모은다 (예외는 클래스명으로 집계)
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
    pool.shutdown();

    // (5) 로스는 예외 수가 아니라 DB 의 최종 재고로 계산한다. 예외를 세는 방식은
    //     "예외 없이 사라진 요청"을 놓치므로, 독립적인 증거(재고)를 기준으로 삼는다.
    int finalQty = hubStockRepository.findById(seeded.getId()).orElseThrow().getQuantity();
    int reflected = INITIAL - finalQty;
    return new Result(
        fail,
        reflected,
        TOTAL - reflected,
        stats.getOptimisticFailureCount(),
        TimeUnit.NANOSECONDS.toMillis(arriveAt[TOTAL - 1]),
        elapsedMs,
        all,
        ok,
        errors);
  }

  // ═══ [F] 측정 헬퍼 ════════════════════════════════════════════════

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
  }

  // ═══ [G] 데이터 헬퍼 ══════════════════════════════════════════════

  // 운영과 같은 도메인 팩토리(HubStock.create)로 재고 1건을 만들어 저장한다.
  // 엔티티 생성자가 protected 라 팩토리를 쓸 수밖에 없고, 그래야 운영과 같은 초기 상태가 된다.
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
