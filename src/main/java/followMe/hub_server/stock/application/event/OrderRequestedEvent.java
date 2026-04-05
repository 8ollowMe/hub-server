package followMe.hub_server.stock.application.event;

import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest.Product;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
public class OrderRequestedEvent {
  private Payload payload;
  private String eventId;
  private String eventType;
  private String domainType;
  private String domainId;
  private String occurredAt;
  private String correlationId;

  @ToString
  @Getter
  @AllArgsConstructor
  public static class Payload {
    private UUID orderId;
    private List<Product> products;

    @ToString
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Product {
      UUID id;
      Integer quantity;
    }
  }

  public OrderHubStockRequest toRequest() {
    return OrderHubStockRequest.builder()
        .orderId(this.payload.getOrderId())
        .products(
            this.payload.getProducts().stream()
                .map(
                    item -> Product.builder().id(item.getId()).quantity(item.getQuantity()).build())
                .toList())
        .build();
  }
}
