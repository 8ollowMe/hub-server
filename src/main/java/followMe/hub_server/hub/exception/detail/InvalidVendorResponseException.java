package followMe.hub_server.hub.exception.detail;

import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.HubException;

public class InvalidVendorResponseException extends HubException {

    public InvalidVendorResponseException() {
        super(HubErrorCode.INVALID_VENDOR_RESPONSE);
    }
}
