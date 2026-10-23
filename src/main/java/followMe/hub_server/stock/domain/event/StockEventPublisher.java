package followMe.hub_server.stock.domain.event;

public interface StockEventPublisher {
  void stockChangedEventPublish(StockChangedEvent stockChangedEvent);
}
