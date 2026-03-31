package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidHubNameException extends HubException {
    public InvalidHubNameException() {
        super(HubErrorCode.INVALID_HUB_NAME);
    }

    public InvalidHubNameException(String message) {
        super(HubErrorCode.INVALID_HUB_NAME, message);
    }
}
