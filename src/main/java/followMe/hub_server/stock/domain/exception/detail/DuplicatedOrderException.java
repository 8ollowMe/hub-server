package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class DuplicatedOrderException extends StockException {
  public DuplicatedOrderException() {
    super(StockErrorCode.ORDER_DUPLICATED_PRODUCT);
  }
}
