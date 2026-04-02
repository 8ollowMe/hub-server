package followMe.hub_server.stock.application.dto;

import followMe.hub_server.stock.domain.HubStock;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
@Builder
public class SearchResponse {
  UUID productId;
  String productCode;
  String productName;
  UUID hubId;
  Integer quantity;

  public static Page<SearchResponse> from(Page<HubStock> hubStocks) {
    return hubStocks.map(
        stock ->
            SearchResponse.builder()
                .productId(stock.getProductId())
                .productCode(stock.getProductInfo().getProductCode())
                .productName(stock.getProductInfo().getProductName())
                .hubId(stock.getHubId())
                .quantity(stock.getQuantity())
                .build());
  }
}
