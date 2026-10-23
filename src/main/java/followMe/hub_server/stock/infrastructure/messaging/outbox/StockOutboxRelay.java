package followMe.hub_server.stock.infrastructure.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression(
    "'${event.publish.mode:}' == 'polling' or '${event.publish.mode:}' == 'hybrid'")
@RequiredArgsConstructor
public class StockOutboxRelay {

  private final StockOutboxRepository repository;
  private final StockOutboxStatusUpdater statusUpdater;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${outbox.relay.send-timeout-ms:5000}")
  private long sendTimeoutMs;

  @Value("${outbox.relay.min-age-ms:0}")
  private long minAgeMs;

  // 테스트에서 "릴레이 프로세스가 죽은 상태"를 만들기 위한 스위치 (운영 기본값 true)
  private final AtomicBoolean schedulingEnabled = new AtomicBoolean(true);

  public void setSchedulingEnabled(boolean enabled) {
    schedulingEnabled.set(enabled);
  }

  @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:1000}")
  public void poll() {
    if (schedulingEnabled.get()) {
      relayOnce();
    }
  }

  public synchronized int relayOnce() {
    List<StockOutbox> batch =
        repository.findTop50ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            StockOutbox.Status.PENDING, Instant.now().minusMillis(minAgeMs));
    int sent = 0;
    for (StockOutbox o : batch) {
      try {
        kafkaTemplate
            .send(o.getTopic(), o.getAggregateKey(), o.getPayload())
            .get(sendTimeoutMs, TimeUnit.MILLISECONDS); // 브로커 ack까지 동기 대기
        statusUpdater.markPublished(o.getId());
        sent++;
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        log.warn("[outbox-relay] publish failed eventId={} cause={}", o.getId(), ex.toString());
        statusUpdater.markAttemptFailed(o.getId(), ex.toString());
        break; // 뒤의 이벤트가 앞질러 나가면 순서가 깨지므로 이번 배치는 중단
      }
    }
    return sent;
  }
}
