package followMe.hub_server.stock.domain;

import com.followMe.common.entity.BaseAudit;
import followMe.hub_server.stock.domain.vo.ProductInfo;
import followMe.hub_server.stock.domain.vo.Vendor;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "p_hub_stock_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class HubStockHistory extends BaseAudit {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "history_id", nullable = false)
  private UUID historyId;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Embedded private ProductInfo productInfo;

  @Embedded private Vendor vendor;

  @Column(name = "hub_id", nullable = false)
  private UUID hubId;

  @Column(nullable = false)
  private Type type;

  @Column(name = "ref_id")
  private UUID refId;

  @Column(name = "before_quantity", nullable = false)
  private Integer beforeQuantity;

  @Column(name = "after_quantity", nullable = false)
  private Integer afterQuantity;

  @Builder(access = AccessLevel.PRIVATE)
  public HubStockHistory(
      UUID productId,
      ProductInfo productInfo,
      Vendor vendor,
      UUID hubId,
      Type type,
      UUID refId,
      Integer beforeQuantity,
      Integer afterQuantity) {
    this.productId = productId;
    this.productInfo = productInfo;
    this.vendor = vendor;
    this.hubId = hubId;
    this.type = type;
    this.refId = refId;
    this.beforeQuantity = beforeQuantity;
    this.afterQuantity = afterQuantity;
  }

  public static HubStockHistory record(
      HubStock hubStock, Type type, UUID refId, Integer beforeQuantity, Integer afterQuantity) {
    return HubStockHistory.builder()
        .productId(hubStock.getProductId())
        .productInfo(hubStock.getProductInfo())
        .vendor(hubStock.getVendor())
        .hubId(hubStock.getHubId())
        .type(type)
        .refId(refId)
        .beforeQuantity(beforeQuantity)
        .afterQuantity(afterQuantity)
        .build();
  }
}
