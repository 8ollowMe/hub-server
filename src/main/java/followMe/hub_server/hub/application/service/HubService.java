package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.command.CreateHubCommand;
import followMe.hub_server.hub.application.dto.command.GetHubsQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubCommand;
import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import followMe.hub_server.hub.application.dto.result.HubResult;
import java.util.UUID;

public interface HubService {

  HubResult createHub(UserContext authUser, CreateHubCommand command);

  HubResult getHub(UUID hubId);

  GetHubsPageResult searchHubs(GetHubsQuery query);

  HubResult updateHub(UserContext authUser, UpdateHubCommand command);

  HubResult deleteHub(UserContext authUser, UUID hubId);
}
