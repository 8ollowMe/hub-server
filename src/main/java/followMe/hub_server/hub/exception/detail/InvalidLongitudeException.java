package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidLongitudeException extends HubException {
  public InvalidLongitudeException() {
    super(HubErrorCode.INVALID_LONGITUDE);
  }

  public InvalidLongitudeException(String message) {
    super(HubErrorCode.INVALID_LONGITUDE, message);
  }
}
