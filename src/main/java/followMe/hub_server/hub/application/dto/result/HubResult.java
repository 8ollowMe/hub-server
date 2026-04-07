package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.domain.entity.Hub;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HubResult(
    UUID hubId,
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt)
    implements Serializable {
  public static HubResult from(Hub hub) {
    return new HubResult(
        hub.getHubId(),
        hub.getHubName(),
        hub.getAddress(),
        hub.getLatitude(),
        hub.getLongitude(),
        hub.getCreatedAt(),
        hub.getUpdatedAt(),
        hub.getDeletedAt());
  }
}
