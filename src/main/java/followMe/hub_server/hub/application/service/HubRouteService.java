package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.result.HubRouteResult;

import java.util.UUID;

public interface HubRouteService {

    HubRouteResult getRoute(UUID sourceHubId, UUID vendorId);
}
