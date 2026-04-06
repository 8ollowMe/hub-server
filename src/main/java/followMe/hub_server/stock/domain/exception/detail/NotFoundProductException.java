package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class NotFoundProductException extends StockException {
  public NotFoundProductException() {
    super(StockErrorCode.PRODUCT_NOT_FOUND);
  }
}
