package followMe.hub_server.stock.presentation;

import com.followMe.common.pagination.PageRequest;
import com.followMe.common.pagination.PageResponse;
import com.followMe.common.response.ApiResponse;
import followMe.hub_server.stock.application.dto.history.SearchStockHistoryResponse;
import followMe.hub_server.stock.application.service.QueryHubStockHistoryService;
import followMe.hub_server.stock.application.service.UserContext;
import followMe.hub_server.stock.application.service.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "재고 관리 내역", description = "허브 재고 관리 내역 관련 외부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/stock-histories/")
public class HubStockHistoryController {
  private final QueryHubStockHistoryService queryHubStockHistoryService;

  @ModelAttribute
  public UserContext userContext(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-Role") UserRole role,
      @RequestHeader(value = "X-Hub-Id", required = false) UUID hubId,
      @RequestHeader(value = "X-Vendor-Id", required = false) UUID vendorId,
      @RequestHeader(value = "X-User-Name", required = false) String userName) {
    return new UserContext(userId, role, hubId, vendorId, userName);
  }

  @Operation(
      summary = "재고 관리 내역 조회",
      description =
          "재고 관리 내역을 조회 합니다. "
              + "<br>로그인한 모든 사용자가 접근 가능합니다."
              + "<br>상품 식별자를 통해 해당 상품의 재고 변동 기록을 조회 합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse> searchStockHistory(
      UUID productId, PageRequest pageRequest, @ModelAttribute UserContext authUser, Sort sort) {

    Page<SearchStockHistoryResponse> responses =
        queryHubStockHistoryService.getStockHistory(
            productId, pageRequest.toPageable(ifNotSortedReturnCreatedAtDesc(sort)));
    return ApiResponse.ok(PageResponse.of(responses));
  }

  private Sort ifNotSortedReturnCreatedAtDesc(Sort sort) {
    return sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "createdAt");
  }
}
