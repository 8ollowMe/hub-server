package followMe.hub_server.hub.application.dto.command;

import java.math.BigDecimal;

public record CreateHubCommand(
    String name, String address, BigDecimal latitude, BigDecimal longitude) {}
