package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class HubAlreadyDeletedException extends HubException {
    public HubAlreadyDeletedException() {
        super(HubErrorCode.HUB_ALREADY_DELETED);
    }
}
