package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidAddressException extends HubException {
    public InvalidAddressException() {
        super(HubErrorCode.INVALID_ADDRESS);
    }

    public InvalidAddressException(String message) {
        super(HubErrorCode.INVALID_ADDRESS, message);
    }
}
