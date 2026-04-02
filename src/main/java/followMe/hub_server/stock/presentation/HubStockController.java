package followMe.hub_server.stock.presentation;

import com.followMe.common.pagination.PageRequest;
import com.followMe.common.response.ApiResponse;
import followMe.hub_server.hub.application.service.UserContext;
import followMe.hub_server.hub.application.service.UserRole;
import followMe.hub_server.stock.application.dto.CreateHubStockDto.CreateHubStockRequest;
import followMe.hub_server.stock.application.dto.CreateHubStockDto.CreateHubStockResponse;
import followMe.hub_server.stock.application.dto.DeleteHubStockDto.DeleteHubStockRequest;
import followMe.hub_server.stock.application.dto.SearchCondition;
import followMe.hub_server.stock.application.dto.SearchResponse;
import followMe.hub_server.stock.application.dto.UpdateHubStockDto.UpdateHubStockRequest;
import followMe.hub_server.stock.application.dto.UpdateHubStockDto.UpdateHubStockResponse;
import followMe.hub_server.stock.application.service.HubStockService;
import followMe.hub_server.stock.application.service.QueryHubStockService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class HubStockController {
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

  @PostMapping
  public ApiResponse createHubStock(
      @RequestBody CreateHubStockRequest request, @ModelAttribute UserContext authUser) {

    CreateHubStockResponse response = hubStockService.create(request, authUser.userId());
    return ApiResponse.success(response);
  }

  @GetMapping
  public ApiResponse getHubStocks(
      SearchCondition condition, PageRequest pageRequest, @ModelAttribute UserContext authUser) {

    Page<SearchResponse> response =
        queryHubStockService.searchHubStock(condition, pageRequest.toPageable());
    return ApiResponse.success(response);
  }

  @PatchMapping("/{productId}")
  public ApiResponse updateHubStock(
      @PathVariable UUID productId,
      @RequestBody UpdateHubStockRequest request,
      @ModelAttribute UserContext authUser) {

    UpdateHubStockResponse response = hubStockService.update(request, productId, authUser.userId());
    return ApiResponse.success(response);
  }

  @DeleteMapping("/{productId}")
  public ApiResponse deleteHubStock(
      @PathVariable UUID productId,
      @RequestBody DeleteHubStockRequest request,
      @ModelAttribute UserContext authUser) {

    hubStockService.delete(request, productId, authUser.userId());
    return ApiResponse.success();
  }
}
