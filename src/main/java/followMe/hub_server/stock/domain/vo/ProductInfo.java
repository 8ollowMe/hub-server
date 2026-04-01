package followMe.hub_server.stock.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductInfo {
  @Column(name = "product_Code", length = 50, nullable = false)
  private String productCode;

  @Column(name = "product_name", nullable = false)
  private String productName;

  public static ProductInfo of(String productCode, String productName) {
    return new ProductInfo(productCode, productName);
  }
}
