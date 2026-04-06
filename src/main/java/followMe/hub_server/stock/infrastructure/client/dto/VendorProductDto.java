package followMe.hub_server.stock.infrastructure.client.dto;

import java.util.Objects;
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

    public boolean equals(UUID productId, UUID hubId, UUID vendorId) {
      return Objects.equals(productId, this.productId)
          && Objects.equals(hubId, this.hubId)
          && Objects.equals(vendorId, this.vendorId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(productId, hubId, vendorId);
    }
  }

  public enum ProductStatus {
    ON_SALE,
    SOLD_OUT,
    DISCONTINUED,
    PENDING,
  }
}
