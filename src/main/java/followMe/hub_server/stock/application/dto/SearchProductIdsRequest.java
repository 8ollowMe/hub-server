package followMe.hub_server.stock.application.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchProductIdsRequest {
  private List<RequestProduct> products;

  @Getter
  @AllArgsConstructor
  public static class RequestProduct {
    private UUID id;
  }
}
