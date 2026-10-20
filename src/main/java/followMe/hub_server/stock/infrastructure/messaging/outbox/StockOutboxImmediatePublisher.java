package followMe.hub_server.stock.infrastructure.messaging.outbox;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "event.publish.mode", havingValue = "hybrid")
@RequiredArgsConstructor
public class StockOutboxImmediatePublisher {

  private final StockOutboxStatusUpdater statusUpdater;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${outbox.relay.send-timeout-ms:5000}")
  private long sendTimeoutMs;

  private volatile boolean enabled = true; // 테스트용 스위치 (운영 기본값 true)

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void publishAsync(StockOutbox outbox) {
    if (!enabled) return;
    // 요청 스레드와 분리: 브로커가 느려도 사용자 응답은 지연되지 않는다.
    Thread.startVirtualThread(() -> publish(outbox));
  }

  private void publish(StockOutbox o) {
    try {
      kafkaTemplate
          .send(o.getTopic(), o.getAggregateKey(), o.getPayload())
          .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
      statusUpdater.markPublished(o.getId());
    } catch (Exception e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      log.warn("[outbox-immediate] publish failed eventId={} cause={}", o.getId(), e.toString());
      statusUpdater.markAttemptFailed(o.getId(), e.toString()); // PENDING 유지 → 릴레이가 재시도
    }
  }
}
