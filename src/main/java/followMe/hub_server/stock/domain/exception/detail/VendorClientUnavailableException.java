package followMe.hub_server.stock.domain.exception.detail;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;

public class VendorClientUnavailableException extends StockException {
  public VendorClientUnavailableException() {
    super(StockErrorCode.VENDOR_CLIENT_UNAVAILABLE);
  }

  public VendorClientUnavailableException(StockErrorCode errorCode) {
    super(errorCode);
  }
}
