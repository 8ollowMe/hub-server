package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.domain.entity.HubRoute;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HubRouteDetailResult(
    UUID hubRouteId,
    UUID originHubId,
    String originHubName,
    UUID destinationHubId,
    String destinationHubName,
    BigDecimal duration,
    BigDecimal distance,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt)
    implements Serializable {

  public static HubRouteDetailResult from(HubRoute route) {
    return new HubRouteDetailResult(
        route.getHubRouteId(),
        route.getOriginHub().getHubId(),
        route.getOriginHub().getHubName(),
        route.getDestinationHub().getHubId(),
        route.getDestinationHub().getHubName(),
        route.getDuration(),
        route.getDistance(),
        route.getCreatedAt(),
        route.getUpdatedAt(),
        route.getDeletedAt());
  }
}
