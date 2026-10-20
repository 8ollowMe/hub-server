package followMe.hub_server.stock.infrastructure.messaging.outbox;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnExpression("'${event.publish.mode:}' == 'polling' or '${event.publish.mode:}' == 'hybrid'")
@RequiredArgsConstructor
public class StockOutboxStatusUpdater {

  private final StockOutboxRepository repository;

  @Transactional
  public void markPublished(UUID id) {
    repository.findById(id).ifPresent(StockOutbox::markPublished);
  }

  @Transactional
  public void markAttemptFailed(UUID id, String error) {
    repository.findById(id).ifPresent(o -> o.markAttemptFailed(error));
  }
}
