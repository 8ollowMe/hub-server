package followMe.hub_server.stock.application.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
public class ProductUpdatedEvent {

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
    private UUID productId;
    private String productCode;
    private String productName;
  }
}
