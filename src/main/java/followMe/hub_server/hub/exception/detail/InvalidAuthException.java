package followMe.hub_server.hub.exception.detail;

import com.followMe.common.exception.ErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidAuthException extends HubException {

  public InvalidAuthException(ErrorCode errorCode) {
    super(errorCode);
  }

  public InvalidAuthException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
