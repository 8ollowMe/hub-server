package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class DestinationHubRequiredException extends HubException {
  public DestinationHubRequiredException() {
    super(HubErrorCode.DESTINATION_HUB_REQUIRED);
  }
}
