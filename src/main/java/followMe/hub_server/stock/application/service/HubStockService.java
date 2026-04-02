package followMe.hub_server.stock.application.service;

import followMe.hub_server.stock.application.dto.CreateHubStockDto.CreateHubStockRequest;
import followMe.hub_server.stock.application.dto.CreateHubStockDto.CreateHubStockResponse;
import followMe.hub_server.stock.application.dto.DeleteHubStockDto.DeleteHubStockRequest;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockRequest.Product;
import followMe.hub_server.stock.application.dto.OrderHubStockDto.OrderHubStockResponse;
import followMe.hub_server.stock.application.dto.UpdateHubStockDto.UpdateHubStockRequest;
import followMe.hub_server.stock.application.dto.UpdateHubStockDto.UpdateHubStockResponse;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
import followMe.hub_server.stock.domain.exception.detail.DuplicatedOrderException;
import followMe.hub_server.stock.domain.exception.detail.DuplicatedProductException;
import followMe.hub_server.stock.domain.exception.detail.NotFoundProductException;
import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import followMe.hub_server.stock.domain.service.PermissionChecker;
import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class HubStockService {
  private final HubStockRepository hubStockRepository;

  private final PermissionChecker permissionChecker;
  private final HubExistenceChecker hubExistenceChecker;
  private final VendorProductExistenceChecker vendorProductExistenceChecker;

  /*
   * 상품 재고 생성
   * - 이미 있는 상품을 중복하여 재고 생성 시 예외 반환
   */
  public CreateHubStockResponse create(CreateHubStockRequest request, UUID requesterId) {

    boolean isExistProduct = hubStockRepository.existsById(request.getProductId());
    if (isExistProduct) {
      throw new DuplicatedProductException();
    }

    HubStock saved =
        hubStockRepository.save(
            HubStock.create(
                requesterId,
                request.getProductId(),
                request.getProductCode(),
                request.getProductName(),
                request.getHubId(),
                request.getVendorId(),
                request.getVendorName(),
                request.getQuantity(),
                permissionChecker,
                hubExistenceChecker,
                vendorProductExistenceChecker));

    return CreateHubStockResponse.from(saved);
  }

  /*
   * 상품 재고 단건 수정
   * - 관리 목적의 상품 재고 수정
   */
  public UpdateHubStockResponse update(
      UpdateHubStockRequest request, UUID productId, UUID requesterId) {

    HubStock stock =
        hubStockRepository.findById(productId).orElseThrow(NotFoundProductException::new);

    // 추후 리팩토링 필요
    if (HubStock.isDecreaseType(request.getType())) {
      stock.decreaseQuantity(
          requesterId, request.getQuantity(), request.getType(), permissionChecker);

    } else if (HubStock.isIncreaseType(request.getType())) {
      stock.increaseQuantity(
          requesterId, request.getQuantity(), request.getType(), permissionChecker);
    }

    return UpdateHubStockResponse.from(stock);
  }

  /*
   * 주문으로 인한 재고 수정
   */
  public OrderHubStockResponse order(OrderHubStockRequest request, UUID requesterId) {
    List<Product> products = request.getProducts();
    checkOrderDuplicateIds(products);

    Map<UUID, Integer> requestMap =
        products.stream().collect(Collectors.toMap(Product::getId, Product::getQuantity));

    Collection<HubStock> stocks = hubStockRepository.findAllByProductIdIn(requestMap.keySet());
    UUID orderId = request.getOrderId();

    // 조회한 상품 갯수와, 요청 갯수가 맞지않는 경우 -> DB에 존재하지않는 상품 존재
    if (stocks.size() != requestMap.size()) {
      throw new NotFoundProductException();
    }

    for (HubStock stock : stocks) {
      UUID p = stock.getProductId();
      Integer requestQuantity = requestMap.get(p);
      stock.orderDecreaseQuantity(orderId, requestQuantity);
    }

    return OrderHubStockResponse.from(orderId, Instant.now());
  }

  /*
   * 주문 취소 인한 재고 롤백
   * TODO: 주문 요청, 취소 같은 로직. 추후 리팩토링 필요
   */
  public OrderHubStockResponse orderCancel(OrderHubStockRequest request, UUID requesterId) {
    List<Product> products = request.getProducts();
    checkOrderDuplicateIds(products);

    Map<UUID, Integer> requestMap =
        products.stream().collect(Collectors.toMap(Product::getId, Product::getQuantity));

    Collection<HubStock> stocks = hubStockRepository.findAllByProductIdIn(requestMap.keySet());
    UUID orderId = request.getOrderId();

    if (stocks.size() != requestMap.size()) {
      throw new NotFoundProductException();
    }

    for (HubStock stock : stocks) {
      UUID p = stock.getProductId();
      Integer requestQuantity = requestMap.get(p);
      stock.orderCancelQuantity(orderId, requestQuantity);
    }

    return OrderHubStockResponse.from(orderId, Instant.now());
  }

  /*
   * 상품 재고 삭제
   */
  public void delete(DeleteHubStockRequest request, UUID productId, UUID requesterId) {

    HubStock stock =
        hubStockRepository.findById(productId).orElseThrow(NotFoundProductException::new);

    stock.delete(requesterId, request.getType(), permissionChecker);
  }

  /*
   * 중복 상품 요청 검사
   */
  private void checkOrderDuplicateIds(List<Product> products) {
    long uniqueCount = products.stream().map(Product::getId).distinct().count();
    if (uniqueCount != products.size()) {
      throw new DuplicatedOrderException();
    }
  }
}
