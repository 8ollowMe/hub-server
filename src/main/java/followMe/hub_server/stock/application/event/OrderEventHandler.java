package followMe.hub_server.stock.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.followMe.common.event.inbox.Inbox;
import com.followMe.common.event.inbox.InboxRepository;
import followMe.hub_server.stock.application.service.HubStockService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventHandler {
  private static final String ORDER_REQUESTED_EVENT = "OrderRequestedEvent";
  private static final String ORDER_CANCELLED_EVENT = "OrderCancelledEvent";

  private static final Map<String, Class<?>> eventMap =
      Map.of(
          ORDER_REQUESTED_EVENT,
          OrderRequestedEvent.class,
          ORDER_CANCELLED_EVENT,
          OrderCancelledEvent.class);

  private final ObjectMapper objectMapper;
  private final InboxRepository inboxRepository;
  private final HubStockService hubStockService;

  /*
   * 주문 요청 이벤트 리스너
   *
   * 주문 요청이 들어오면 재고가 수정되어야 함
   *
   * 처리 실패시 에러 로그만 출력.
   * 계속된 처리 실패를 반복하지 않기위해 Inbox 기록
   *
   * 추후 재시도 정책 필요
   */
  @KafkaListener(topics = ORDER_REQUESTED_EVENT, groupId = "hub-stock-group11")
  public void orderRequestedHandle(String strEvent) throws Exception {
    OrderRequestedEvent event =
        (OrderRequestedEvent) eventMapped(strEvent, eventMap.get(ORDER_REQUESTED_EVENT));
    UUID eventId = UUID.fromString(event.getEventId());

    if (inboxRepository.existsByIdAndMessageGroup(eventId, ORDER_REQUESTED_EVENT)) {
      return;
    }

    log.debug("payload: {}", event.getPayload());
    try {
      hubStockService.order(event.toRequest(), null);
    } catch (Exception e) {
      log.error("상품 재고 차감 실패. eventId: {}, cause: {}", eventId, e.getMessage());
    }

    inboxRepository.save(Inbox.builder().id(eventId).messageGroup(ORDER_REQUESTED_EVENT).build());
  }

  /*
   * 주문 취소 이벤트 리스너
   *
   * 주문 요청이 들어오면 재고가 복구되어야 함
   *
   * 처리 실패시 에러 로그만 출력.
   * 계속된 처리 실패를 반복하지 않기위해 Inbox 기록
   *
   * 추후 재시도 정책 필요
   */
  @KafkaListener(topics = ORDER_CANCELLED_EVENT, groupId = "hub-stock-group11")
  public void setOrderCancelledHandle(String strEvent) throws Exception {
    OrderCancelledEvent event =
        (OrderCancelledEvent) eventMapped(strEvent, eventMap.get(ORDER_CANCELLED_EVENT));
    UUID eventId = UUID.fromString(event.getEventId());

    if (inboxRepository.existsByIdAndMessageGroup(eventId, ORDER_CANCELLED_EVENT)) {
      return;
    }

    log.debug("payload: {}", event.getPayload());
    try {
      hubStockService.orderCancel(event.toRequest(), null);
    } catch (Exception e) {
      log.error("주문 취소에 따른 재고 복구 실패. eventId: {}, cause: {}", eventId, e.getMessage());
    }

    inboxRepository.save(Inbox.builder().id(eventId).messageGroup(ORDER_CANCELLED_EVENT).build());
  }

  private <T> T eventMapped(String strEvent, Class<T> clazz) throws Exception {
    String json = objectMapper.readValue(strEvent, String.class);
    return objectMapper.readValue(json, clazz);
  }
}
