package followMe.hub_server.hub.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateHubRouteCommand(
        UUID originHubId,
        UUID destinationHubId,
        BigDecimal duration,
        BigDecimal distance
) {}
