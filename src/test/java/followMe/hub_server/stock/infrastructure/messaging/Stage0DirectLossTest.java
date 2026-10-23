package followMe.hub_server.stock.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import followMe.hub_server.common.audit.AuditorContext;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import followMe.hub_server.stock.domain.service.PermissionChecker;
import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
    properties = {
      "event.publish.mode=direct", // 직접 발행
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.kafka.producer.properties.max.block.ms=2000", // send()가 메타데이터를 기다리는 최대 시간
      "spring.kafka.producer.properties.delivery.timeout.ms=3000", // 전송 전체 제한 시간
      "spring.kafka.producer.properties.request.timeout.ms=2000" // 요청 1회 응답 제한 시간
    })
@Testcontainers
class Stage0DirectLossTest {

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

  // 테스트 메서드에 @Transactional을 붙이지 않는다.
  // 붙이면 테스트가 끝날 때 롤백되어 AFTER_COMMIT 리스너가 호출되지 않는다.
  @Autowired TransactionTemplate tx;
  @Autowired HubStockRepository repo;

  @BeforeEach
  void setUp() {
    when(permissionChecker.hasCreatePermission(any(), any(), any())).thenReturn(true);
    when(hubExistenceChecker.hasHub(any())).thenReturn(true);
    when(vendorProductExistenceChecker.hasVendorProduct(any(), any(), any())).thenReturn(true);
  }

  // ── 시나리오 1: 브로커가 멈춘 채 재고를 커밋하면 이벤트는 사라진다 ─────────────────────
  @Test
  void 브로커_장애중_커밋하면_이벤트는_유실된다(CapturedOutput output) {
    UUID productId = UUID.randomUUID();
    log.debug("[STAGE0][1/5] 시나리오: 브로커 장애 중 재고 커밋 productId={}", productId);

    log.debug("[STAGE0][2/5] 브로커 일시정지 (pause)");
    kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();
    try {
      log.debug("[STAGE0][3/5] 재고 생성 트랜잭션 커밋 → AFTER_COMMIT에서 send() 시도");
      createStock(productId);
    } finally {
      kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
      log.debug("[STAGE0][4/5] 브로커 재개 (unpause)");
    }

    // (a) 비즈니스 데이터는 정상 커밋됐다 → 사용자 입장에서는 "성공"한 요청
    boolean saved = repo.existsById(productId);
    // (b) 토픽에서 이 productId의 이벤트를 10초간 찾는다
    int arrived =
        KafkaTestSupport.drain(
                kafka.getBootstrapServers(),
                DirectStockEventPublisher.TOPIC,
                productId.toString(),
                Duration.ofSeconds(10))
            .size();
    log.debug("[STAGE0][5/5] 결과: DB 저장={}, 토픽 도착={}건", saved, arrived);

    assertThat(saved).isTrue(); // DB에는 남아 있고
    assertThat(arrived).isZero(); // 토픽에는 0건 → 이벤트 유실

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> assertThat(output.getOut()).contains("[direct-publish] kafka publish failed"));
    log.debug("[STAGE0] 결론: 요청은 성공, 이벤트는 유실, 호출자는 장애를 알 수 없음 (로그에만 남음)");
  }

  // ── 시나리오 2: 대조군 — 브로커가 정상이면 같은 동작으로 이벤트가 도착한다 ───────────────
  @Test
  void 대조군_브로커가_정상이면_이벤트가_도착한다() {
    UUID productId = UUID.randomUUID();
    log.debug("[STAGE0][CONTROL] 브로커 정상 상태에서 동일하게 재고 생성 productId={}", productId);

    createStock(productId);

    var records =
        KafkaTestSupport.drain(
            kafka.getBootstrapServers(),
            DirectStockEventPublisher.TOPIC,
            productId.toString(),
            Duration.ofSeconds(10));
    log.debug(
        "[STAGE0][CONTROL] 토픽 도착={}건 payload={}",
        records.size(),
        records.isEmpty() ? "-" : records.get(0).value());

    assertThat(records).hasSize(1); // 1건 도착해야 시나리오 1의 "0건"이 장애 탓임이 증명된다
  }

  private void createStock(UUID productId) {
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
        });
  }
}
