package followMe.hub_server.stock.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import followMe.hub_server.common.audit.AuditorContext;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import followMe.hub_server.stock.domain.service.PermissionChecker;
import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutbox;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxRelay;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxRepository;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxStatusUpdater;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@SpringBootTest(
    properties = {
      "event.publish.mode=polling", // 검증 대상: 폴링 전용 Outbox
      "outbox.relay.interval-ms=1000",
      "outbox.relay.send-timeout-ms=4000",
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.kafka.producer.properties.max.block.ms=2000",
      "spring.kafka.producer.properties.delivery.timeout.ms=3000",
      "spring.kafka.producer.properties.request.timeout.ms=2000",
      "spring.jpa.properties.hibernate.generate_statistics=true" // S4'에서 쿼리 수를 세기 위함
    })
@Testcontainers
class Stage1PollingOutboxTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15");

  @Container
  static KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @MockitoBean PermissionChecker permissionChecker;
  @MockitoBean HubExistenceChecker hubExistenceChecker;
  @MockitoBean VendorProductExistenceChecker vendorProductExistenceChecker;
  @MockitoSpyBean StockOutboxStatusUpdater statusUpdater; // S5에서 일부러 실패시키기 위한 spy

  @Autowired TransactionTemplate tx;
  @Autowired HubStockRepository repo;
  @Autowired StockOutboxRepository outboxRepo;
  @Autowired StockOutboxRelay relay;
  @Autowired EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    relay.setSchedulingEnabled(true);
    when(permissionChecker.hasCreatePermission(any(), any(), any())).thenReturn(true);
    when(hubExistenceChecker.hasHub(any())).thenReturn(true);
    when(vendorProductExistenceChecker.hasVendorProduct(any(), any(), any())).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    relay.setSchedulingEnabled(true); // 다음 테스트에 영향이 없도록 항상 복구
  }

  // ── S1 ───────────────────────────────────────────────────────────
  @Test
  void S1_브로커_장애중_커밋해도_outbox에_남았다가_복구후_전달된다() {
    UUID productId = UUID.randomUUID();
    log.debug("[STAGE1][S1] 브로커 pause → 재고 커밋");
    kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();
    try {
      createStock(productId, false);
      List<StockOutbox> rows = outboxOf(productId);
      log.debug(
          "[STAGE1][S1] 장애 중 outbox 상태={}", rows.stream().map(StockOutbox::getStatus).toList());
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).getStatus()).isEqualTo(StockOutbox.Status.PENDING); // 이벤트가 DB에 남아 있음
    } finally {
      kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
    }

    log.debug("[STAGE1][S1] 브로커 재개 → 릴레이 재전송 대기");
    await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () ->
                assertThat(outboxOf(productId).get(0).getStatus())
                    .isEqualTo(StockOutbox.Status.PUBLISHED));

    String eventId = outboxOf(productId).get(0).getId().toString();
    List<ConsumerRecord<String, String>> records = drain(productId, 5);
    log.debug(
        "[STAGE1][S1] 결과: 토픽 도착={}건 (Stage 0은 0건) retryCount={}",
        records.size(),
        outboxOf(productId).get(0).getRetryCount());
    // 장애 중 타임아웃된 전송이 복구 후 뒤늦게 도착해 중복될 수 있으므로(at-least-once) 1건 이상 + 모두 같은 eventId
    assertThat(records).isNotEmpty().allSatisfy(r -> assertThat(r.value()).contains(eventId));
  }

  // ── S2 ───────────────────────────────────────────────────────────
  @Test
  void S2_커밋직후_릴레이가_죽어도_outbox에_남아_재기동후_전달된다() {
    UUID productId = UUID.randomUUID();
    relay.setSchedulingEnabled(false); // 릴레이(프로세스)가 죽은 상태를 흉내
    log.debug("[STAGE1][S2] 릴레이 정지 상태에서 커밋");
    createStock(productId, false);

    assertThat(outboxOf(productId)).hasSize(1);
    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PENDING);
    assertThat(drain(productId, 3)).isEmpty();
    log.debug("[STAGE1][S2] 크래시 직후: DB 커밋됨, outbox=PENDING, 토픽 0건");

    log.debug("[STAGE1][S2] 릴레이 재기동(relayOnce)");
    relay.relayOnce();

    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PUBLISHED);
    List<ConsumerRecord<String, String>> records = drain(productId, 5);
    log.debug("[STAGE1][S2] 결과: 재기동 후 토픽 도착={}건", records.size());
    assertThat(records).hasSize(1);
  }

  // ── S3 ───────────────────────────────────────────────────────────
  @Test
  void S3_커밋전_예외로_롤백되면_재고도_outbox도_이벤트도_없다() {
    UUID productId = UUID.randomUUID();
    log.debug("[STAGE1][S3] 재고 저장 후 커밋 전 예외 발생");
    assertThatThrownBy(() -> createStock(productId, true))
        .isInstanceOf(IllegalStateException.class);

    boolean stockExists = repo.existsById(productId);
    int outboxRows = outboxOf(productId).size();
    int arrived = drain(productId, 3).size();
    log.debug("[STAGE1][S3] 결과: 재고={}, outbox={}건, 토픽={}건", stockExists, outboxRows, arrived);

    assertThat(stockExists).isFalse();
    assertThat(outboxRows).isZero(); // 없는 변경의 이벤트가 만들어지지 않음
    assertThat(arrived).isZero();
  }

  // ── S4 ───────────────────────────────────────────────────────────
  @Test
  void S4_커밋에서_토픽_도착까지_지연은_폴링간격_이내다() throws Exception {
    long sum = 0, max = 0;
    int n = 5;
    for (int i = 0; i < n; i++) {
      UUID productId = UUID.randomUUID();
      long t0 = System.currentTimeMillis();
      createStock(productId, false);
      List<ConsumerRecord<String, String>> records = drain(productId, 3);
      assertThat(records).isNotEmpty();
      long latency = records.get(0).timestamp() - t0; // 레코드 timestamp = 프로듀서 전송 시각
      log.debug("[STAGE1][S4] #{} 커밋→전송 지연={}ms", i + 1, latency);
      sum += latency;
      max = Math.max(max, latency);
      Thread.sleep(300); // 폴링 주기와 어긋나게 시작하려는 간격
    }
    log.debug("[STAGE1][S4] 결과: 평균={}ms, 최대={}ms (폴링 간격 1000ms)", sum / n, max);
    assertThat(max).isLessThan(3000);
  }

  // ── S4' ──────────────────────────────────────────────────────────
  @Test
  void S4prime_유휴상태에서도_폴링_쿼리가_주기적으로_나간다() throws Exception {
    await()
        .atMost(Duration.ofSeconds(30))
        .until(
            () ->
                outboxRepo
                    .findTop50ByStatusOrderByCreatedAtAsc(StockOutbox.Status.PENDING)
                    .isEmpty());

    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    Thread.sleep(5000);
    long queries = stats.getQueryExecutionCount();
    log.debug("[STAGE1][S4'] 결과: 유휴 5초 동안 폴링 쿼리={}회 (간격 1000ms)", queries);
    assertThat(queries).isBetween(3L, 8L);
  }

  // ── S5 ───────────────────────────────────────────────────────────
  @Test
  void S5_발행성공_후_상태갱신이_실패하면_같은_이벤트가_중복_발행된다() {
    UUID productId = UUID.randomUUID();
    relay.setSchedulingEnabled(false);
    createStock(productId, false);

    // 첫 호출만 실패, 이후엔 실제 메서드 실행
    doThrow(new RuntimeException("DB 일시 장애"))
        .doCallRealMethod()
        .when(statusUpdater)
        .markPublished(any());

    log.debug("[STAGE1][S5] 1차 relay: 전송 성공, 상태 갱신 실패");
    relay.relayOnce();
    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PENDING);
    assertThat(drain(productId, 3)).hasSize(1);

    log.debug("[STAGE1][S5] 2차 relay: PENDING이라 다시 전송");
    relay.relayOnce();
    String eventId = outboxOf(productId).get(0).getId().toString();
    List<ConsumerRecord<String, String>> records = drain(productId, 5);
    log.debug("[STAGE1][S5] 결과: 같은 eventId={} 가 토픽에 {}건", eventId, records.size());

    assertThat(records).hasSize(2).allSatisfy(r -> assertThat(r.value()).contains(eventId));
    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PUBLISHED);
  }

  // ── 공통 ─────────────────────────────────────────────────────────
  private void createStock(UUID productId, boolean failAfterSave) {
    tx.executeWithoutResult(
        s -> {
          AuditorContext.setCurrentUserId(UUID.randomUUID());
          repo.save(
              HubStock.create(
                  UUID.randomUUID(),
                  productId,
                  "P-1",
                  "name",
                  UUID.randomUUID(),
                  UUID.randomUUID(),
                  "vendor",
                  10,
                  permissionChecker,
                  hubExistenceChecker,
                  vendorProductExistenceChecker));
          if (failAfterSave) {
            throw new IllegalStateException("의도된 실패: 커밋 전 롤백");
          }
        });
  }

  private List<StockOutbox> outboxOf(UUID productId) {
    return outboxRepo.findAll().stream()
        .filter(o -> o.getAggregateKey().equals(productId.toString()))
        .toList();
  }

  private List<ConsumerRecord<String, String>> drain(UUID productId, int seconds) {
    return KafkaTestSupport.drain(
        kafka.getBootstrapServers(),
        DirectStockEventPublisher.TOPIC,
        productId.toString(),
        Duration.ofSeconds(seconds));
  }
}
