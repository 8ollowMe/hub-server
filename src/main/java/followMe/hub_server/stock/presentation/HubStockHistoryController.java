package followMe.hub_server.stock.presentation;

import com.followMe.common.pagination.PageRequest;
import com.followMe.common.pagination.PageResponse;
import com.followMe.common.response.ApiResponse;
import followMe.hub_server.stock.application.dto.history.SearchStockHistoryResponse;
import followMe.hub_server.stock.application.service.QueryHubStockHistoryService;
import followMe.hub_server.stock.application.service.UserContext;
import followMe.hub_server.stock.application.service.UserRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/stock-histories/")
public class HubStockHistoryController {
  private final QueryHubStockHistoryService queryHubStockHistoryService;

  @ModelAttribute
  public UserContext userContext(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") UserRole role,
      @RequestHeader(value = "X-Hub-Id", required = false) UUID hubId,
      @RequestHeader(value = "X-Vendor-Id", required = false) UUID vendorId,
      @RequestHeader(value = "X-User-Name", required = false) String userName) {
    return new UserContext(userId, role, hubId, vendorId, userName);
  }

  @GetMapping
  public ResponseEntity<ApiResponse> searchStockHistory(
      UUID productId, PageRequest pageRequest, @ModelAttribute UserContext authUser) {

    Page<SearchStockHistoryResponse> responses =
        queryHubStockHistoryService.getStockHistory(productId, pageRequest.toPageable());
    return ApiResponse.ok(PageResponse.of(responses));
  }
}
