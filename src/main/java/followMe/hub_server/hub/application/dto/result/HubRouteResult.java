package followMe.hub_server.hub.application.dto.result;

import java.io.Serializable;
import java.util.List;

public record HubRouteResult(List<RouteNodeResult> nodes) implements Serializable {}
