package followMe.hub_server.stock.infrastructure.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import followMe.hub_server.stock.domain.event.StockChangedEvent;
import followMe.hub_server.stock.infrastructure.messaging.DirectStockEventPublisher;
import followMe.hub_server.stock.infrastructure.messaging.StockChangedMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnExpression(
    "'${event.publish.mode:}' == 'polling' or '${event.publish.mode:}' == 'hybrid'")
@RequiredArgsConstructor
public class StockOutboxRecorder {

  private final StockOutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<StockOutboxImmediatePublisher> immediatePublisher;

  // BEFORE_COMMIT: 재고 변경과 "같은 트랜잭션" 안에서 실행된다.
  //  - 예외가 나면 재고 변경까지 롤백된다 (이벤트 없이 커밋되는 경우가 없음)
  //  - 재고 변경이 롤백되면 이 INSERT도 함께 롤백된다 (없는 변경의 이벤트가 나가지 않음)
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void on(StockChangedEvent e) throws JsonProcessingException {
    StockChangedMessage m = StockChangedMessage.from(e);
    StockOutbox outbox =
        StockOutbox.pending(
            UUID.fromString(m.eventId()),
            m.productId().toString(),
            DirectStockEventPublisher.TOPIC,
            objectMapper.writeValueAsString(m));
    outboxRepository.save(outbox);

    StockOutboxImmediatePublisher publisher = immediatePublisher.getIfAvailable();
    if (publisher != null) {
      // 커밋이 성공한 뒤에만 즉시 발행한다. 롤백되면 이 콜백은 호출되지 않는다.
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              publisher.publishAsync(outbox);
            }
          });
    }
  }
}
