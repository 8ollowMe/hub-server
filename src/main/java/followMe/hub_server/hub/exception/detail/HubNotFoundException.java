package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class HubNotFoundException extends HubException {
  public HubNotFoundException() {
    super(HubErrorCode.HUB_NOT_FOUND);
  }
}
