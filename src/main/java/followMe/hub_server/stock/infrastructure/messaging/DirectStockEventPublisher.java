package followMe.hub_server.stock.infrastructure.messaging;

import followMe.hub_server.stock.domain.event.StockChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "event.publish.mode", havingValue = "direct")
@RequiredArgsConstructor
public class DirectStockEventPublisher {
  public static final String TOPIC = "hub.stock.changed";
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(StockChangedEvent e) {
    StockChangedMessage m = StockChangedMessage.from(e);
    kafkaTemplate.send(TOPIC, m.productId().toString(), m);
  }
}
