package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class NotFoundUserException extends StockException {
  public NotFoundUserException() {
    super(StockErrorCode.USER_NOT_FOUND);
  }
}
