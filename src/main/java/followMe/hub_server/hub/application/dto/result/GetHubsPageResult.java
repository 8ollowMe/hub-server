package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.domain.entity.Hub;
import java.util.List;
import org.springframework.data.domain.Page;

public record GetHubsPageResult(
    List<HubResult> content, int page, int size, long totalElements, int totalPages) {
  public static GetHubsPageResult from(Page<Hub> page) {
    return new GetHubsPageResult(
        page.getContent().stream().map(HubResult::from).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
