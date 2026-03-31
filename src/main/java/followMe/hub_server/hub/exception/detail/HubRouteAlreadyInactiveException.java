package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class HubRouteAlreadyInactiveException extends HubException {
    public HubRouteAlreadyInactiveException() {
        super(HubErrorCode.HUB_ROUTE_ALREADY_INACTIVE);
    }
}