package followMe.hub_server.stock.domain.event;

import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.Type;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StockOrderEvent {
  private HubStock hubStock;
  private Type type;
  private UUID orderId;
  private Integer beforeQuantity;
  private Integer afterQuantity;

  public static StockOrderEvent of(
      HubStock hubStock, UUID orderId, Integer beforeQuantity, Integer afterQuantity) {
    return StockOrderEvent.builder()
        .hubStock(hubStock)
        .type(Type.OUTBOUND)
        .orderId(orderId)
        .beforeQuantity(beforeQuantity)
        .afterQuantity(afterQuantity)
        .build();
  }
}
