package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class HubRouteAlreadyExistsException extends HubException {
  public HubRouteAlreadyExistsException() {
    super(HubErrorCode.HUB_ROUTE_ALREADY_EXISTS);
  }
}
