package followMe.hub_server.stock.application.dto;

import com.followMe.common.util.TimeUtil;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.Type;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class UpdateHubStockDto {

  @Getter
  @AllArgsConstructor
  public static class UpdateHubStockRequest {
    Type type;
    Integer quantity;
  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class UpdateHubStockResponse {
    private UUID productId;
    private LocalDateTime updatedAt;

    public static UpdateHubStockResponse from(HubStock hubStock) {
      return UpdateHubStockResponse.builder()
          .productId(hubStock.getProductId())
          .updatedAt(TimeUtil.toLocalDateTime(hubStock.getCreatedAt()))
          .build();
    }
  }
}
