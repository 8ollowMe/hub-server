package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class InvalidQuantityException extends StockException {
  public InvalidQuantityException() {
    super(StockErrorCode.STOCK_INVALID_QUANTITY);
  }

  public InvalidQuantityException(StockErrorCode errorCode) {
    super(errorCode);
  }
}
