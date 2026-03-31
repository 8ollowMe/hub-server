package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class SameOriginDestinationException extends HubException {
  public SameOriginDestinationException() {
    super(HubErrorCode.SAME_ORIGIN_DESTINATION);
  }
}
