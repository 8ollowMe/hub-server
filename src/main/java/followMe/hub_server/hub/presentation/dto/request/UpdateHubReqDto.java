package followMe.hub_server.hub.presentation.dto.request;

import followMe.hub_server.hub.application.dto.command.UpdateHubCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateHubReqDto(
    String name,
    String address,
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude) {
  public UpdateHubCommand toCommand(UUID hubId) {
    return new UpdateHubCommand(hubId, name, address, latitude, longitude);
  }
}
