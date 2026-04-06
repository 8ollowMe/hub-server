package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class NotFoundHubException extends StockException {
  public NotFoundHubException() {
    super(StockErrorCode.HUB_NOT_FOUND);
  }
}
