package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import java.io.Serializable;
import java.util.UUID;

public record RouteNodeResult(UUID id, NodeType type, String name, int sequence)
    implements Serializable {}
