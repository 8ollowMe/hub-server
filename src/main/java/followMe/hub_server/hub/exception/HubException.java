package followMe.hub_server.hub.exception;

import com.followMe.common.exception.BusinessException;
import com.followMe.common.exception.ErrorCode;

public class HubException extends BusinessException {

  public HubException(ErrorCode errorCode) {
    super(errorCode);
  }

  public HubException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
