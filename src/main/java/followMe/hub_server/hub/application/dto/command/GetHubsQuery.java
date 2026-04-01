package followMe.hub_server.hub.application.dto.command;

import com.followMe.common.pagination.PageRequest;

public record GetHubsQuery(String keyword, PageRequest pageRequest) {}
