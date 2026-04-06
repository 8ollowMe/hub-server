package followMe.hub_server.stock.application.dto;

import com.followMe.common.util.TimeUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class OrderHubStockDto {

  @Getter
  @AllArgsConstructor
  public static class OrderHubStockRequest {
    private UUID orderId;
    private List<Product> products;

    @Getter
    @AllArgsConstructor
    public static class Product {
      private UUID id;
      private Integer quantity;
    }
  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class OrderHubStockResponse {
    private UUID orderId;
    private LocalDateTime completedAt;

    public static OrderHubStockResponse from(UUID orderId, Instant completedAt) {
      return OrderHubStockResponse.builder()
          .orderId(orderId)
          .completedAt(TimeUtil.toLocalDateTime(completedAt))
          .build();
    }
  }
}
