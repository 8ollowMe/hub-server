package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class UserClientUnavailableException extends StockException {
  public UserClientUnavailableException() {
    super(StockErrorCode.USER_CLIENT_UNAVAILABLE);
  }

  public UserClientUnavailableException(StockErrorCode errorCode) {
    super(errorCode);
  }
}
