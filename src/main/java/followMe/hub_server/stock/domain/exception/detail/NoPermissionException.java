package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class NoPermissionException extends StockException {
  public NoPermissionException(StockErrorCode stockErrorCode) {
    super(stockErrorCode);
  }
}
