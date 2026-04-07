package followMe.hub_server.hub.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateHubRouteCommand(
        UUID hubRouteId,
        UUID originHubId,
        UUID destinationHubId,
        BigDecimal duration,
        BigDecimal distance
) {}
