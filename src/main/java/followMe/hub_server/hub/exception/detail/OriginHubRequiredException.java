package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class OriginHubRequiredException extends HubException {
  public OriginHubRequiredException() {
    super(HubErrorCode.ORIGIN_HUB_REQUIRED);
  }
}
