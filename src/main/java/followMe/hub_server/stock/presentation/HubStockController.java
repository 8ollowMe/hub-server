package followMe.hub_server.stock.presentation;

import com.followMe.common.pagination.PageRequest;
import com.followMe.common.pagination.PageResponse;
import com.followMe.common.response.ApiResponse;
import followMe.hub_server.stock.application.dto.CreateHubStockDto.CreateHubStockRequest;
import followMe.hub_server.stock.application.dto.CreateHubStockDto.CreateHubStockResponse;
import followMe.hub_server.stock.application.dto.DeleteHubStockDto.DeleteHubStockRequest;
import followMe.hub_server.stock.application.dto.SearchCondition;
import followMe.hub_server.stock.application.dto.SearchResponse;
import followMe.hub_server.stock.application.dto.UpdateHubStockDto.UpdateHubStockRequest;
import followMe.hub_server.stock.application.dto.UpdateHubStockDto.UpdateHubStockResponse;
import followMe.hub_server.stock.application.service.HubStockService;
import followMe.hub_server.stock.application.service.QueryHubStockService;
import followMe.hub_server.stock.application.service.UserContext;
import followMe.hub_server.stock.application.service.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "재고", description = "허브 재고 관리 관련 외부 API")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class HubStockController {
  private final HubStockService hubStockService;
  private final QueryHubStockService queryHubStockService;

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
      summary = "재고 등록",
      description =
          "초기 상품 재고를 등록합니다.<br>'MASTER', 'HUB(담당 허브)' 권한을 가진 사용자만 접근 가능합니다."
              + "<br>재고의 생성, 수정, 삭제 모든 과정은 stock-history를 통해 기록됩니다.")
  @PostMapping
  public ResponseEntity<ApiResponse> createHubStock(
      @RequestBody CreateHubStockRequest request, @ModelAttribute UserContext authUser) {

    CreateHubStockResponse response = hubStockService.create(request, authUser.userId());
    return ApiResponse.ok(response);
  }

  @Operation(
      summary = "재고 조회",
      description =
          "재고들을 조회, 검색 합니다. "
              + "<br>로그인 한 사용자만 접근 가능합니다. "
              + "<br>상품 식별자, 허브 식별자, 업체 식별자 별로 검색 조회를 제공합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse> getHubStocks(
      SearchCondition condition, PageRequest pageRequest, @ModelAttribute UserContext authUser) {

    Page<SearchResponse> response =
        queryHubStockService.searchHubStock(condition, pageRequest.toPageable());
    return ApiResponse.ok(PageResponse.of(response));
  }

  @Operation(
      summary = "재고 변경",
      description =
          "재고 입고, 출고 내역(INBOUND(입고), OUTBOUND(출고), ADJUST_LOSS(파손), RETURN_IN(반품), DISCONTINUED(단종), ORDER_CANCELED(주문 취소))을"
              + "<br> 기록하고, 재고를 수정합니다."
              + "<br>'MASTER', 'HUB(담당 허브)' 권한을 가진 사용자만 접근 가능합니다.")
  @PatchMapping("/{productId}")
  public ResponseEntity<ApiResponse> updateHubStock(
      @PathVariable UUID productId,
      @RequestBody UpdateHubStockRequest request,
      @ModelAttribute UserContext authUser) {

    UpdateHubStockResponse response = hubStockService.update(request, productId, authUser.userId());
    return ApiResponse.ok(response);
  }

  @Operation(
      summary = "재고 삭제",
      description =
          "재고 목록에서 상품을 삭제합니다."
              + "<br> 재고가 남아있는 상품은 삭제 할 수 없습니다."
              + "<br>'MASTER', 'HUB(담당 허브)' 권한을 가진 사용자만 접근 가능합니다.")
  @DeleteMapping("/{productId}")
  public ResponseEntity<ApiResponse> deleteHubStock(
      @PathVariable UUID productId,
      @RequestBody DeleteHubStockRequest request,
      @ModelAttribute UserContext authUser) {

    hubStockService.delete(request, productId, authUser.userId());
    return ApiResponse.ok();
  }
}
