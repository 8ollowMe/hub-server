package followMe.hub_server.stock.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import followMe.hub_server.stock.domain.event.StockChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@ConditionalOnProperty(name = "event.publish.mode", havingValue = "direct")
@RequiredArgsConstructor
public class DirectStockEventPublisher {

  public static final String TOPIC = "hub.stock.changed";

  private final KafkaTemplate<String, String> kafkaTemplate; // Object -> String
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(StockChangedEvent e) {
    StockChangedMessage m = StockChangedMessage.from(e);
    try {
      String payload = objectMapper.writeValueAsString(m);
      kafkaTemplate
          .send(TOPIC, m.productId().toString(), payload)
          .whenComplete(
              (r, ex) -> { // 비동기 실패 (delivery timeout 등)
                if (ex != null) {
                  log.error("[direct-publish] kafka publish failed eventId={}", m.eventId(), ex);
                }
              });
    } catch (Exception ex) { // 동기 실패 (metadata 타임아웃, 직렬화)
      log.error("[direct-publish] kafka publish failed eventId={}", m.eventId(), ex);
    }
  }
}
