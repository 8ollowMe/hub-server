package followMe.hub_server.stock.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.followMe.common.event.inbox.Inbox;
import com.followMe.common.event.inbox.InboxRepository;
import followMe.hub_server.stock.application.event.ProductUpdatedEvent;
import followMe.hub_server.stock.application.event.ProductUpdatedEvent.Payload;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Service
@RequiredArgsConstructor
public class ProductEventHandler {
  private static final String PRODUCT_UPDATED_EVENT = "ProductUpdatedEvent";
  private static final Map<String, Class<?>> eventMap =
      Map.of(PRODUCT_UPDATED_EVENT, ProductUpdatedEvent.class);

  private final ObjectMapper objectMapper;
  private final InboxRepository inboxRepository;
  private final HubStockService hubStockService;

  /*
   * 상품 정보 변경 이벤트 리스너
   *
   * 재고 테이블의 상품 정보도 수정되어야 함 (상품 코드, 상품 이름)
   *
   * 처리 실패시 에러 로그만 출력.
   * 계속된 처리 실패를 반복하지 않기위해 Inbox 기록
   *
   * 추후 재시도 정책 필요
   */
  @KafkaListener(topics = PRODUCT_UPDATED_EVENT, groupId = "hub-stock-group10")
  public void consume(String strEvent) throws Exception {
    ProductUpdatedEvent event =
        (ProductUpdatedEvent) eventMapped(strEvent, eventMap.get(PRODUCT_UPDATED_EVENT));

    UUID eventId = UUID.fromString(event.getEventId());

    if (inboxRepository.existsByIdAndMessageGroup(eventId, PRODUCT_UPDATED_EVENT)) {
      return;
    }

    Payload payload = event.getPayload();
    log.debug("payload: {}", event.getPayload());
    try {
      hubStockService.updateStockInfo(
          payload.getProductId(), payload.getProductCode(), payload.getProductName());
    } catch (Exception e) {
      log.error(
          "상품 정보 업데이트에 실패했습니다. productId: {}, cause: {}", payload.getProductId(), e.getMessage());
    }

    inboxRepository.save(Inbox.builder().id(eventId).messageGroup(PRODUCT_UPDATED_EVENT).build());
  }

  private <T> T eventMapped(String strEvent, Class<T> clazz) throws Exception {
    String json = objectMapper.readValue(strEvent, String.class);
    return objectMapper.readValue(json, clazz);
  }
}
