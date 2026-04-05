package followMe.hub_server.stock.infrastructure.client.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class VendorProductDto {

  @Builder
  @Getter
  @AllArgsConstructor
  public static class VendorProductInfo {
    UUID productId;
    UUID hubId;
    UUID vendorId;
    String vendorName;
    String name;
    Integer price;
    String code;
    String description;
    ProductStatus status;
  }

  public enum ProductStatus {
    ON_SALE,
    SOLD_OUT,
    DISCONTINUED,
    PENDING,
  }
}
