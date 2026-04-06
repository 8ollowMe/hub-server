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
public class StockChangedEvent {
  private HubStock hubStock;
  private Type type;
  private UUID refId;
  private Integer beforeQuantity;
  private Integer afterQuantity;

  public static StockChangedEvent inboundOf(
      HubStock hubStock, Integer beforeQuantity, Integer afterQuantity) {
    return StockChangedEvent.builder()
        .hubStock(hubStock)
        .type(Type.INBOUND)
        .refId(null)
        .beforeQuantity(beforeQuantity)
        .afterQuantity(afterQuantity)
        .build();
  }

  public static StockChangedEvent decreaseOf(
      HubStock hubStock, Type type, Integer beforeQuantity, Integer afterQuantity) {
    return StockChangedEvent.builder()
        .hubStock(hubStock)
        .type(type)
        .refId(null)
        .beforeQuantity(beforeQuantity)
        .afterQuantity(afterQuantity)
        .build();
  }

  public static StockChangedEvent increaseOf(
      HubStock hubStock, Type type, Integer beforeQuantity, Integer afterQuantity) {
    return StockChangedEvent.builder()
        .hubStock(hubStock)
        .type(type)
        .refId(null)
        .beforeQuantity(beforeQuantity)
        .afterQuantity(afterQuantity)
        .build();
  }

  public static StockChangedEvent deleteOf(HubStock hubStock) {
    return StockChangedEvent.builder()
        .hubStock(hubStock)
        .type(Type.DISCONTINUED)
        .refId(null)
        .beforeQuantity(0)
        .afterQuantity(0)
        .build();
  }
}
