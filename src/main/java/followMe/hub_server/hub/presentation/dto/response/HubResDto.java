package followMe.hub_server.hub.presentation.dto.response;

import followMe.hub_server.hub.application.dto.result.HubResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HubResDto(
    UUID hubId,
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt) {
  public static HubResDto from(HubResult result) {
    return new HubResDto(
        result.hubId(),
        result.name(),
        result.address(),
        result.latitude(),
        result.longitude(),
        result.createdAt(),
        result.updatedAt(),
        result.deletedAt());
  }
}
