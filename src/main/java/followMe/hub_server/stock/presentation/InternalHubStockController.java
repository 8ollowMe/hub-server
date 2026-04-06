package followMe.hub_server.stock.presentation;

import com.followMe.common.pagination.PageRequest;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockResponse;
import followMe.hub_server.stock.application.dto.SearchProductIdsRequest;
import followMe.hub_server.stock.application.dto.SearchResponse;
import followMe.hub_server.stock.application.service.HubStockService;
import followMe.hub_server.stock.application.service.QueryHubStockService;
import followMe.hub_server.stock.application.service.UserContext;
import followMe.hub_server.stock.application.service.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "재고 내부 API", description = "허브 재고 관리 관련 내부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/stocks")
public class InternalHubStockController {
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
      summary = "재고 다건 조회",
      description = "상품 식별자 리스트를 통해 상품의 재고 리스트를 조회 합니다." + "<br>로그인한 모든 사용자가 접근 가능합니다.")
  @GetMapping
  public List<SearchResponse> getStocks(
      @RequestBody SearchProductIdsRequest request,
      PageRequest pageRequest,
      @ModelAttribute UserContext authUser) {

    List<SearchResponse> responses = queryHubStockService.searchHubStock(request);
    return responses;
  }

  @Operation(
      summary = "주문을 통한 재고 다건 변경",
      description = "주문 식별자, 상품 리스트, 상품 수량 리스트를 통해 다건의 재고를 감소 시킵니다." + "<br>로그인한 모든 사용자가 접근 가능합니다.")
  @PatchMapping("/order")
  public OrderHubStockResponse requestStockOrder(
      @RequestBody OrderHubStockRequest request, @ModelAttribute UserContext authUser) {

    OrderHubStockResponse response = hubStockService.order(request, authUser.userId());
    return response;
  }

  @Operation(
      summary = "주문 취소를 통한 재고 다건 변경",
      description = "주문 식별자, 상품 리스트, 상품 수량 리스트를 통해 다건의 재고를 복구 합니다." + "<br>로그인한 모든 사용자가 접근 가능합니다.")
  @PatchMapping("/cancel")
  public OrderHubStockResponse cancelStockOrder(
      @RequestBody OrderHubStockRequest request, @ModelAttribute UserContext authUser) {

    OrderHubStockResponse response = hubStockService.orderCancel(request, authUser.userId());
    return response;
  }
}
