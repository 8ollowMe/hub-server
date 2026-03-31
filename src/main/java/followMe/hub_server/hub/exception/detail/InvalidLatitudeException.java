package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidLatitudeException extends HubException {
  public InvalidLatitudeException() {
    super(HubErrorCode.INVALID_LATITUDE);
  }

  public InvalidLatitudeException(String message) {
    super(HubErrorCode.INVALID_LATITUDE, message);
  }
}
