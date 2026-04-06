package followMe.hub_server.stock.application.dto;

import com.followMe.common.util.TimeUtil;
import followMe.hub_server.stock.domain.HubStock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class CreateHubStockDto {

  @Getter
  @AllArgsConstructor
  public static class CreateHubStockRequest {
    private UUID productId;
    private String productCode;
    private String productName;
    private UUID hubId;
    private UUID vendorId;
    private String vendorName;
    private Integer quantity;
  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class CreateHubStockResponse {
    private UUID productId;
    private LocalDateTime createdAt;

    public static CreateHubStockResponse from(HubStock hubStock) {
      return CreateHubStockResponse.builder()
          .productId(hubStock.getProductId())
          .createdAt(TimeUtil.toLocalDateTime(hubStock.getCreatedAt()))
          .build();
    }
  }
}
