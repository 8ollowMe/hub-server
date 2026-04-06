package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class DuplicatedProductException extends StockException {
  public DuplicatedProductException() {
    super(StockErrorCode.PRODUCT_DUPLICATED);
  }
}
