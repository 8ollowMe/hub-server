package followMe.hub_server.stock.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchCondition {
  private UUID productId;
  private UUID hubId;
  private UUID vendorId;
}
