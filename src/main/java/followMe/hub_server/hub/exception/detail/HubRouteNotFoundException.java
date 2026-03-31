package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class HubRouteNotFoundException extends HubException {
    public HubRouteNotFoundException() {
        super(HubErrorCode.HUB_ROUTE_NOT_FOUND);
    }
}