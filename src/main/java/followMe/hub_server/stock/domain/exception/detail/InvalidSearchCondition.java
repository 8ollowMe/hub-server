package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class InvalidSearchCondition extends StockException {
  public InvalidSearchCondition() {
    super(StockErrorCode.STOCK_INVALID_SEARCH_CONDITION);
  }
}
