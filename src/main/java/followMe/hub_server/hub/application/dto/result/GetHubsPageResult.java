package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.domain.entity.Hub;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;

public record GetHubsPageResult(
    List<HubResult> content, int page, int size, long totalElements, int totalPages)
    implements Serializable {
  public static GetHubsPageResult from(Page<Hub> page) {
    return new GetHubsPageResult(
        page.getContent().stream().map(HubResult::from).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
