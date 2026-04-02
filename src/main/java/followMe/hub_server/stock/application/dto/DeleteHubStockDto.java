package followMe.hub_server.stock.application.dto;

import followMe.hub_server.stock.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class DeleteHubStockDto {

  @Getter
  @AllArgsConstructor
  public static class DeleteHubStockRequest {
    private Type type;
  }
}
