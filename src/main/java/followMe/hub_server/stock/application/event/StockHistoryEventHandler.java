package followMe.hub_server.stock.application.event;

import followMe.hub_server.common.audit.AuditorContext;
import followMe.hub_server.stock.domain.HubStockHistory;
import followMe.hub_server.stock.domain.HubStockHistoryRepository;
import followMe.hub_server.stock.domain.event.StockChangedEvent;
import followMe.hub_server.stock.domain.event.StockOrderEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockHistoryEventHandler {
  private final HubStockHistoryRepository historyRepository;

  // TODO: retry 도입 시, 보상 행위 필요. 현재는 이력 저장 실패 시 무시
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockChangedEvent(StockChangedEvent event) {

    AuditorContext.setCurrentUserId(UUID.randomUUID());

    historyRepository.save(
        HubStockHistory.record(
            event.getHubStock(),
            event.getType(),
            event.getRefId(),
            event.getBeforeQuantity(),
            event.getAfterQuantity()));
    log.info("Stock changed Record");
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderStockChangedEvent(StockOrderEvent event) {

    AuditorContext.setCurrentUserId(UUID.randomUUID());

    historyRepository.save(
        HubStockHistory.record(
            event.getHubStock(),
            event.getType(),
            event.getOrderId(),
            event.getBeforeQuantity(),
            event.getAfterQuantity()));
    log.info("Order Stock changed Record");
  }
}
