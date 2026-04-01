package followMe.hub_server.hub.presentation.dto.response;

import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import java.util.List;

public record HubPageResDto(
    List<HubResDto> content, int page, int size, long totalElements, int totalPages) {
  public static HubPageResDto from(GetHubsPageResult result) {
    return new HubPageResDto(
        result.content().stream().map(HubResDto::from).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages());
  }
}
