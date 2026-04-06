package followMe.hub_server.stock.application.event;

import static followMe.hub_server.stock.application.dto.OrderHubStockDto.*;
import static followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest.*;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
  private Payload payload;
  private String eventId;
  private String eventType;
  private String domainType;
  private String domainId;
  private String occurredAt;
  private String correlationId;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Payload {
    private UUID orderId;
    private List<CancelledItem> products;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CancelledItem {
    private UUID id;
    private Integer quantity;
  }

  public OrderHubStockRequest toRequest() {
    return builder()
        .orderId(this.payload.getOrderId())
        .products(
            this.payload.getProducts().stream()
                .map(
                    item -> Product.builder().id(item.getId()).quantity(item.getQuantity()).build())
                .toList())
        .build();
  }
}
