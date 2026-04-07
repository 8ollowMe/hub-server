package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.command.CreateHubRouteCommand;
import followMe.hub_server.hub.application.dto.command.SearchHubRoutesQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubRouteCommand;
import followMe.hub_server.hub.application.dto.result.HubRouteDetailResult;
import followMe.hub_server.hub.application.dto.result.HubRoutePageResult;
import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import java.util.UUID;

public interface HubRouteService {
    HubRouteDetailResult create(UserContext userContext, CreateHubRouteCommand command);
    HubRouteDetailResult get(UUID hubRouteId);
    HubRoutePageResult search(SearchHubRoutesQuery query);
    HubRouteDetailResult update(UserContext userContext, UpdateHubRouteCommand command);
    HubRouteDetailResult delete(UserContext userContext, UUID hubRouteId);
  HubRouteResult getRoute(UUID sourceHubId, UUID vendorId);
}
