package followMe.hub_server.stock.presentation;

import com.followMe.common.pagination.PageRequest;
import followMe.hub_server.hub.application.service.UserContext;
import followMe.hub_server.hub.application.service.UserRole;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockResponse;
import followMe.hub_server.stock.application.dto.SearchProductIdsRequest;
import followMe.hub_server.stock.application.dto.SearchResponse;
import followMe.hub_server.stock.application.service.HubStockService;
import followMe.hub_server.stock.application.service.QueryHubStockService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/stocks")
public class InternalHubStockController {
  private final HubStockService hubStockService;
  private final QueryHubStockService queryHubStockService;

  @ModelAttribute
  public UserContext userContext(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") UserRole role,
      @RequestHeader(value = "X-Hub-Id", required = false) UUID hubId,
      @RequestHeader(value = "X-Vendor-Id", required = false) UUID vendorId) {
    return new UserContext(userId, role, hubId, vendorId);
  }

  @GetMapping
  public Page<SearchResponse> getStocks(
      @RequestBody SearchProductIdsRequest request,
      PageRequest pageRequest,
      @ModelAttribute UserContext authUser) {

    Page<SearchResponse> responses =
        queryHubStockService.searchHubStock(request, pageRequest.toPageable());
    return responses;
  }

  @PatchMapping("/order")
  public OrderHubStockResponse requestStockOrder(
      @RequestBody OrderHubStockRequest request, @ModelAttribute UserContext authUser) {

    OrderHubStockResponse response = hubStockService.order(request, authUser.userId());
    return response;
  }

  @PatchMapping("/cancel")
  public OrderHubStockResponse cancelStockOrder(
      @RequestBody OrderHubStockRequest request, @ModelAttribute UserContext authUser) {

    OrderHubStockResponse response = hubStockService.orderCancel(request, authUser.userId());
    return response;
  }
}
