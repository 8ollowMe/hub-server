package followMe.hub_server.hub.presentation.dto.response;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.RouteNodeResult;
import java.util.UUID;

public record HubRouteNodeResDto(UUID id, NodeType type, String name, int sequence) {
  public static HubRouteNodeResDto from(RouteNodeResult result) {
    return new HubRouteNodeResDto(result.id(), result.type(), result.name(), result.sequence());
  }
}
