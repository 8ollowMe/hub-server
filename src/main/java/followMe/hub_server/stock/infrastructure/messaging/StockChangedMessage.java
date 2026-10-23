package followMe.hub_server.stock.infrastructure.messaging;

import followMe.hub_server.stock.domain.event.StockChangedEvent;
import java.time.Instant;
import java.util.UUID;

public record StockChangedMessage(
    String eventId,
    UUID productId,
    String type,
    Integer before,
    Integer after,
    Instant occurredAt) {

  public static StockChangedMessage from(StockChangedEvent e) {
    return new StockChangedMessage(
        UUID.randomUUID().toString(),
        e.getHubStock().getProductId(),
        e.getType().name(),
        e.getBeforeQuantity(),
        e.getAfterQuantity(),
        Instant.now());
  }
}
