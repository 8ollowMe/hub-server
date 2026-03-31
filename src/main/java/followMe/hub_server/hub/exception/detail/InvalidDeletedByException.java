package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidDeletedByException extends HubException {
    public InvalidDeletedByException() {
        super(HubErrorCode.INVALID_DELETED_BY);
    }

    public InvalidDeletedByException(String message) {
        super(HubErrorCode.INVALID_DELETED_BY, message);
    }
}
