package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class InvalidStockTypeException extends StockException {
  public InvalidStockTypeException(StockErrorCode errorCode) {
    super(errorCode);
  }
}
