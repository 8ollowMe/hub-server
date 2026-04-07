package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record RouteNodeResult(
    UUID id,
    NodeType type,
    String name,
    String address,
    BigDecimal duration,
    BigDecimal distance,
    int sequence)
    implements Serializable {}
