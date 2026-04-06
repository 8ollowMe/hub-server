package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class InvalidUserException extends StockException {
  public InvalidUserException() {
    super(StockErrorCode.USER_INVALID_INFO);
  }
}
