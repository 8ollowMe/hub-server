package followMe.hub_server.hub.application.dto.result;

import followMe.hub_server.hub.domain.entity.HubRoute;
import java.io.Serializable;
import org.springframework.data.domain.Page;

public record HubRoutePageResult(
    java.util.List<HubRouteDetailResult> content,
    int page,
    int size,
    long totalElements,
    int totalPages)
    implements Serializable {

  public static HubRoutePageResult from(Page<HubRoute> page) {
    return new HubRoutePageResult(
        page.getContent().stream().map(HubRouteDetailResult::from).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
