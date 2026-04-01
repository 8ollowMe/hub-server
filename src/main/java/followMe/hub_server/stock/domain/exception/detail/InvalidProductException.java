package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class InvalidProductException extends StockException {
  public InvalidProductException() {
    super(StockErrorCode.PRODUCT_INVALID_INFO);
  }
}
