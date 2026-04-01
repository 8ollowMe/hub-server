package followMe.hub_server.hub.application.dto.command;

public record GetHubsQuery(String keyword, int page, int size) {}
