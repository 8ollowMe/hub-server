package followMe.hub_server.hub.presentation.dto.response;

import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import java.util.List;

public record HubRouteResDto(List<HubRouteNodeResDto> nodes) {

  public static HubRouteResDto from(HubRouteResult result) {
    return new HubRouteResDto(result.nodes().stream().map(HubRouteNodeResDto::from).toList());
  }
}
