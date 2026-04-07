package followMe.hub_server.hub.application.dto.command;

import com.followMe.common.pagination.PageRequest;
import java.util.UUID;

public record SearchHubRoutesQuery(
    UUID originHubId, UUID destinationHubId, PageRequest pageRequest) {}
