package followMe.hub_server.stock.domain.exception;

import com.followMe.common.exception.BusinessException;
import com.followMe.common.exception.ErrorCode;

public class StockException extends BusinessException {

  public StockException(ErrorCode errorCode) {
    super(errorCode);
  }

  public StockException(ErrorCode errorCode, String detailMessage) {
    super(errorCode, detailMessage);
  }

  public StockException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
