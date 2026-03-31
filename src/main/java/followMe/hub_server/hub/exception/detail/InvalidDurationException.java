package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidDurationException extends HubException {
  public InvalidDurationException() {
    super(HubErrorCode.INVALID_DURATION);
  }

  public InvalidDurationException(String message) {
    super(HubErrorCode.INVALID_DURATION, message);
  }
}
