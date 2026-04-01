package followMe.hub_server.hub.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateHubCommand(
    UUID hubId, String name, String address, BigDecimal latitude, BigDecimal longitude) {}
