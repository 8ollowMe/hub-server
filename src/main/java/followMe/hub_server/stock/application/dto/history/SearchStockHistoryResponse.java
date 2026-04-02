package followMe.hub_server.stock.application.dto.history;

import com.followMe.common.util.TimeUtil;
import followMe.hub_server.stock.domain.HubStockHistory;
import followMe.hub_server.stock.domain.Type;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
@AllArgsConstructor
public class SearchStockHistoryResponse {
  private UUID productId;
  private String productCode;
  private String productName;
  private UUID vendorId;
  private String vendorName;
  private UUID hubId;
  private Type type;
  private UUID refId;
  private Integer beforeQuantity;
  private Integer afterQuantity;
  private LocalDateTime createdAt;

  public static Page<SearchStockHistoryResponse> from(Page<HubStockHistory> histories) {
    return histories.map(
        history ->
            SearchStockHistoryResponse.builder()
                .productId(history.getProductId())
                .productCode(history.getProductInfo().getProductCode())
                .productName(history.getProductInfo().getProductName())
                .vendorId(history.getVendor().getId())
                .vendorName(history.getVendor().getName())
                .hubId(history.getHubId())
                .type(history.getType())
                .refId(history.getRefId())
                .beforeQuantity(history.getBeforeQuantity())
                .afterQuantity(history.getAfterQuantity())
                .createdAt(TimeUtil.toLocalDateTime(history.getCreatedAt()))
                .build());
  }
}
