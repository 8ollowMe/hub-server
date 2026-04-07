package followMe.hub_server.hub.presentation.dto.request;

import followMe.hub_server.hub.application.dto.command.UpdateHubRouteCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateHubRouteReqDto(
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID originHubId,

        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID destinationHubId,

        @NotNull(message = "소요 시간은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "소요 시간은 0보다 커야 합니다.")
        BigDecimal duration,

        @NotNull(message = "거리 정보는 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "거리는 0보다 커야 합니다.")
        BigDecimal distance
) {
    public UpdateHubRouteCommand toCommand(UUID hubRouteId) {
        return new UpdateHubRouteCommand(
                hubRouteId,
                originHubId,
                destinationHubId,
                duration,
                distance
        );
    }
}