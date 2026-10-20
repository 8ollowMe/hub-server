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
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxImmediatePublisher;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxRelay;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxRepository;
import followMe.hub_server.stock.infrastructure.messaging.outbox.StockOutboxStatusUpdater;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
      "event.publish.mode=hybrid",
      "outbox.relay.interval-ms=5000", // 재시도 릴레이 주기
      "outbox.relay.min-age-ms=3000", // 3초 이상 지난 PENDING만 재시도 대상
      "outbox.relay.send-timeout-ms=4000",
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.kafka.producer.properties.max.block.ms=2000",
      "spring.kafka.producer.properties.delivery.timeout.ms=3000",
      "spring.kafka.producer.properties.request.timeout.ms=2000",
      "spring.jpa.properties.hibernate.generate_statistics=true"
    })
@Testcontainers
class Stage2HybridOutboxTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15");

  @Container
  static KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @Autowired TransactionTemplate tx;
  @Autowired HubStockRepository repo;
  @Autowired StockOutboxRepository outboxRepo;
  @Autowired StockOutboxRelay relay;
  @Autowired EntityManagerFactory emf;

  @Autowired StockOutboxImmediatePublisher immediate;
  @MockitoBean PermissionChecker permissionChecker;
  @MockitoBean HubExistenceChecker hubExistenceChecker;
  @MockitoBean VendorProductExistenceChecker vendorProductExistenceChecker;
  @MockitoSpyBean StockOutboxStatusUpdater statusUpdater;

  @BeforeEach
  void setUp() {
    // 도메인 검증용 체커는 Mock이라 스텁이 없으면 false를 반환한다 (Stage 0과 동일하게 통과시킨다)
    when(permissionChecker.hasCreatePermission(any(), any(), any())).thenReturn(true);
    when(hubExistenceChecker.hasHub(any())).thenReturn(true);
    when(vendorProductExistenceChecker.hasVendorProduct(any(), any(), any())).thenReturn(true);

    immediate.setEnabled(true);
    relay.setSchedulingEnabled(true);
  }

  @AfterEach
  void tearDown() {
    immediate.setEnabled(true);
    relay.setSchedulingEnabled(true);
  }

  @Test
  void H1_브로커_장애중_커밋해도_복구후_릴레이가_재전송한다() {
    // 테스트 순서와 무관하게 같은 조건을 만들기 위한 예열: 토픽 생성 + 프로듀서 메타데이터 캐시
    UUID warm = UUID.randomUUID();
    createStock(warm, false);
    await()
        .atMost(Duration.ofSeconds(15))
        .until(() -> outboxOf(warm).get(0).getStatus() == StockOutbox.Status.PUBLISHED);

    UUID productId = UUID.randomUUID();
    log.debug("[STAGE2][H1] 브로커 pause → 재고 커밋");
    kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();

    long commitMillis = System.currentTimeMillis();

    try {
      createStock(productId, false);
      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () -> // 즉시 발행이 실패해 재시도 횟수가 오른다
              assertThat(outboxOf(productId).get(0).getRetryCount()).isGreaterThanOrEqualTo(1));
      log.debug(
          "[STAGE2][H1] 장애 중 상태={} retryCount={}",
          outboxOf(productId).get(0).getStatus(),
          outboxOf(productId).get(0).getRetryCount());
      assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PENDING);
    } finally {
      kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
    }
    await()
        .atMost(Duration.ofSeconds(90))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () ->
                assertThat(outboxOf(productId).get(0).getStatus())
                    .isEqualTo(StockOutbox.Status.PUBLISHED));
    String eventId = outboxOf(productId).get(0).getId().toString();
    List<ConsumerRecord<String, String>> records = drain(productId, 5);

    records.forEach(
        r ->
            log.debug(
                "[STAGE2][H1] record partition={} offset={} timestamp={} (커밋 시각 기준 +{}ms)",
                r.partition(),
                r.offset(),
                r.timestamp(),
                r.timestamp() - commitMillis));

    Set<String> distinct = records.stream().map(ConsumerRecord::value).collect(Collectors.toSet());
    log.debug(
        "[STAGE2][H1] 결과: 토픽 도착={}건, 고유 payload={}건 (Stage 0은 0건)",
        records.size(),
        distinct.size());
    assertThat(records).isNotEmpty(); // 유실 없음 (최소 1건)
    assertThat(distinct).hasSize(1); // 모든 메시지가 같은 eventId → 소비 측 멱등성으로 보장 필요
  }

  @Test
  void H2_즉시발행_전에_죽어도_릴레이가_재시도한다() {
    UUID productId = UUID.randomUUID();
    immediate.setEnabled(false); // 커밋 직후 즉시 발행 직전의 프로세스 중단을 흉내
    relay.setSchedulingEnabled(false);
    log.debug("[STAGE2][H2] 즉시 발행 중단 상태에서 커밋");
    createStock(productId, false);
    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PENDING);
    assertThat(drain(productId, 3)).isEmpty();
    log.debug("[STAGE2][H2] 중단 직후: DB 커밋됨, outbox=PENDING, 토픽 0건");

    log.debug("[STAGE2][H2] 릴레이 재기동 → 3초 경과한 PENDING 재전송");
    await()
        .atMost(Duration.ofSeconds(15))
        .until(
            () -> {
              relay.relayOnce();
              return outboxOf(productId).get(0).getStatus() == StockOutbox.Status.PUBLISHED;
            });
    List<ConsumerRecord<String, String>> records = drain(productId, 5);
    log.debug("[STAGE2][H2] 결과: 재시도 후 토픽 도착={}건", records.size());
    assertThat(records).hasSize(1);
  }

  @Test
  void H3_커밋전_예외로_롤백되면_즉시발행도_일어나지_않는다() {
    UUID productId = UUID.randomUUID();
    log.debug("[STAGE2][H3] 재고 저장 후 커밋 전 예외 발생");
    assertThatThrownBy(() -> createStock(productId, true))
        .isInstanceOf(IllegalStateException.class);
    boolean stock = repo.existsById(productId);
    int rows = outboxOf(productId).size();
    int arrived = drain(productId, 3).size();
    log.debug("[STAGE2][H3] 결과: 재고={}, outbox={}건, 토픽={}건", stock, rows, arrived);
    assertThat(stock).isFalse();
    assertThat(rows).isZero();
    assertThat(arrived).isZero();
  }

  @Test
  void H4_커밋에서_전송까지_지연은_릴레이_최소_대기보다_짧다() throws Exception {
    createStock(UUID.randomUUID(), false); // 예열 (프로듀서 메타데이터/연결 확립 비용을 측정에서 제외)
    Thread.sleep(2000);
    long sum = 0, max = 0;
    int n = 5;
    for (int i = 0; i < n; i++) {
      UUID productId = UUID.randomUUID();
      long t0 = System.currentTimeMillis();
      createStock(productId, false);
      List<ConsumerRecord<String, String>> records = drain(productId, 3);
      assertThat(records).isNotEmpty();
      long latency = records.get(0).timestamp() - t0;
      log.debug("[STAGE2][H4] #{} 커밋→전송 지연={}ms", i + 1, latency);
      sum += latency;
      max = Math.max(max, latency);
    }
    log.debug("[STAGE2][H4] 결과: 평균={}ms, 최대={}ms (릴레이 최소 대기 3000ms)", sum / n, max);
    assertThat(max).isLessThan(2500); // 릴레이는 3초 전에 못 집으므로 이 값이면 즉시 발행 경로
  }

  @Test
  void H4prime_유휴상태_폴링_쿼리는_줄어든다() throws Exception {
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
    long q = stats.getQueryExecutionCount();
    log.debug("[STAGE2][H4'] 결과: 유휴 5초 동안 폴링 쿼리={}회", q);
    assertThat(q).isLessThanOrEqualTo(3L);
  }

  @Test
  void H5_상태갱신이_실패하면_릴레이가_재전송해_중복된다() {
    UUID productId = UUID.randomUUID();
    relay.setSchedulingEnabled(false);
    doThrow(new RuntimeException("DB 일시 장애"))
        .doCallRealMethod()
        .when(statusUpdater)
        .markPublished(any());
    log.debug("[STAGE2][H5] 즉시 발행: 전송 성공, 상태 갱신 실패");
    createStock(productId, false);
    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> outboxOf(productId).get(0).getRetryCount() >= 1);
    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PENDING);

    log.debug("[STAGE2][H5] 릴레이 재시도: PENDING이라 다시 전송");
    await()
        .atMost(Duration.ofSeconds(15))
        .until(
            () -> {
              relay.relayOnce();
              return outboxOf(productId).get(0).getStatus() == StockOutbox.Status.PUBLISHED;
            });
    String eventId = outboxOf(productId).get(0).getId().toString();
    List<ConsumerRecord<String, String>> records = drain(productId, 5);
    log.debug("[STAGE2][H5] 결과: 같은 eventId={} 가 토픽에 {}건 → 소비 측 멱등성 필요", eventId, records.size());
    assertThat(records).hasSize(2).allSatisfy(r -> assertThat(r.value()).contains(eventId));
  }

  @Test
  void H6_정상_경로에서는_즉시발행과_릴레이가_중복_발행하지_않는다() throws Exception {
    UUID productId = UUID.randomUUID();
    createStock(productId, false);
    Thread.sleep(8000); // 릴레이 주기(5초)를 한 번 이상 지나게 둔다
    int arrived = drain(productId, 3).size();
    log.debug(
        "[STAGE2][H6] 결과: 정상 경로 토픽 도착={}건, 상태={}", arrived, outboxOf(productId).get(0).getStatus());
    assertThat(arrived).isEqualTo(1);
    assertThat(outboxOf(productId).get(0).getStatus()).isEqualTo(StockOutbox.Status.PUBLISHED);
  }

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
