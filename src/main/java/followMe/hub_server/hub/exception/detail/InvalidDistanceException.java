package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidDistanceException extends HubException {
    public InvalidDistanceException() {
        super(HubErrorCode.INVALID_DISTANCE);
    }

    public InvalidDistanceException(String message) {
        super(HubErrorCode.INVALID_DISTANCE, message);
    }
}
