package followMe.hub_server.stock.domain;

import com.followMe.common.entity.BaseAudit;
import followMe.hub_server.stock.application.service.UserRole;
import followMe.hub_server.stock.domain.event.StockChangedEvent;
import followMe.hub_server.stock.domain.event.StockOrderEvent;
import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.detail.*;
import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import followMe.hub_server.stock.domain.service.OrderValidateChecker;
import followMe.hub_server.stock.domain.service.PermissionChecker;
import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import followMe.hub_server.stock.domain.vo.ProductInfo;
import followMe.hub_server.stock.domain.vo.Vendor;
import followMe.hub_server.stock.infrastructure.event.Events;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@Table(name = "p_hub_stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class HubStock extends BaseAudit implements Persistable<UUID> {
  private static final Set<Type> DECREASE_TYPES = Set.of(Type.ADJUST_LOSS, Type.OUTBOUND);
  private static final Set<Type> INCREASE_TYPES =
      Set.of(Type.INBOUND, Type.RETURN_IN, Type.ORDER_CANCELED);
  private static final Set<Type> DELETED_TYPES = Set.of(Type.DISCONTINUED);
  private static final Set<UserRole> PERMISSION_ROLE = Set.of(UserRole.HUB, UserRole.MASTER);

  @Id
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Embedded private ProductInfo productInfo;

  @Embedded private Vendor vendor;

  @Column(name = "hub_id", nullable = false)
  private UUID hubId;

  @Column(nullable = false)
  Integer quantity;

  @Version Long version;

  @Override
  public UUID getId() {
    return this.productId;
  }

  @Override
  public boolean isNew() {
    return this.getCreatedAt() == null;
  }

  @Builder(access = AccessLevel.PRIVATE)
  private HubStock(
      UUID productId, ProductInfo productInfo, UUID hubId, Vendor vendor, Integer quantity) {
    this.productId = productId;
    this.productInfo = productInfo;
    this.hubId = hubId;
    this.vendor = vendor;
    this.quantity = quantity;
  }

  /*
   * 재고 생성 시 검증
   * 0. 옳바른 초기 재고 수량인지 검증
   * 1. 허브 존재 유무 검증
   * 2. 재고 등록 권한 검증 - 마스터, 허브 관리자(본인 허브)
   * 3. 업체 상품 검증(허브가 관리하는 업체의 상품인지)
   */
  public static HubStock create(
      UUID requesterId,
      UUID productId,
      String productCode,
      String productName,
      UUID hubId,
      UUID vendorId,
      String vendorName,
      Integer quantity,
      PermissionChecker permissionChecker,
      HubExistenceChecker hubExistenceChecker,
      VendorProductExistenceChecker vendorProductExistenceChecker) {

    checkValidateCreateQuantity(quantity);
    checkHubExistence(hubId, hubExistenceChecker);
    checkCreatePermission(requesterId, hubId, permissionChecker);
    checkVendorProductExistence(vendorId, productId, hubId, vendorProductExistenceChecker);

    HubStock stock =
        HubStock.builder()
            .productId(productId)
            .productInfo(ProductInfo.of(productCode, productName))
            .hubId(hubId)
            .vendor(Vendor.of(vendorId, vendorName))
            .quantity(quantity)
            .build();

    Events.publish(StockChangedEvent.inboundOf(stock, 0, quantity));
    return stock;
  }

  /*
   * 재고 감소 시 검증
   * 1. 재고 감소 타입(유형) 검증 & 남은 재고 검증 (현재 재고 > 감소 요청)
   * 3. 재고 감소 권한 검증(주문이 아닌 감소는 마스터, 허브 관리자(본인 허브))
   */
  public void decreaseQuantity(
      UUID requesterId, Integer amount, Type cause, PermissionChecker permissionChecker) {

    checkValidateDecrease(amount, cause);
    checkUpdatePermission(requesterId, this.hubId, permissionChecker);

    Integer before = this.quantity;
    this.quantity -= amount;
    Events.publish(StockChangedEvent.decreaseOf(this, cause, before, this.quantity));
  }

  /*
   * 주문 재고 감소 시 검증
   * 1. 재고 감소 타입(유형) 검증 & 남은 재고 검증 (현재 재고 > 감소 요청)
   * 2. TODO: (제외)orderId 유효성 검증, 필요하다면 application 영역에서 수행.
   *     도메인에서 치리시 N건의 요청에 N건 통신 발생
   */
  public void orderDecreaseQuantity(UUID orderId, Integer amount) {

    checkValidateDecrease(amount, Type.OUTBOUND);

    Integer before = this.quantity;
    this.quantity -= amount;
    Events.publish(StockOrderEvent.of(this, orderId, before, this.quantity));
  }

  public void orderCancelQuantity(UUID orderId, Integer amount) {

    checkValidateIncrease(amount, Type.ORDER_CANCELED);

    Integer before = this.quantity;
    this.quantity += amount;
    Events.publish(StockOrderEvent.cancelOf(this, orderId, before, this.quantity));
  }

  /*
   * 재고 증가 시 검증
   * 1. 재고 증가 타입(유형) 검증
   * 2. 재고 증가 권한 검증(주문이 아닌 감소는 마스터, 허브 관리자(본인 허브))
   */
  public void increaseQuantity(
      UUID requesterId, Integer amount, Type cause, PermissionChecker permissionChecker) {

    checkValidateIncrease(amount, cause);
    checkUpdatePermission(requesterId, this.hubId, permissionChecker);

    Integer before = this.quantity;
    this.quantity += amount;
    Events.publish(StockChangedEvent.increaseOf(this, cause, before, this.quantity));
  }

  /*
   * Vendor Server 에서 상품 정보 업데이트 시 처리 할 도메인 로직
   * Stock History는 기록을 목적으로하기때문에, 변경 사항 미전파
   */
  public void updateStockInfo(String productCode, String productName) {
    this.productInfo = ProductInfo.of(productCode, productName);
  }

  /*
   * 재고 삭제 시 검증
   * 1. 재고 수량, 타입 검증(재고가 남아있으면 삭제 불가)
   * 2. 재고 삭제 권한 검증
   */
  public void delete(UUID requesterId, Type cause, PermissionChecker permissionChecker) {

    checkValidateDeleteStock(cause);
    checkDeletePermission(requesterId, this.hubId, permissionChecker);

    this.softDelete();
    Events.publish(StockChangedEvent.deleteOf(this));
  }

  public static boolean isDecreaseType(Type type) {
    if (Objects.isNull(type)) {
      throw new InvalidStockTypeException(StockErrorCode.STOCK_TYPE_NULL);
    }
    return DECREASE_TYPES.contains(type);
  }

  public static boolean isIncreaseType(Type type) {
    if (Objects.isNull(type)) {
      throw new InvalidStockTypeException(StockErrorCode.STOCK_TYPE_NULL);
    }
    return INCREASE_TYPES.contains(type);
  }

  private static void checkValidateCreateQuantity(Integer quantity) {
    if (Objects.isNull(quantity) || quantity < 0) {
      throw new InvalidQuantityException(StockErrorCode.STOCK_INVALID_INIT_QUANTITY);
    }
  }

  /*
   * MASTER - 가능
   * HUB - 자신의 담당 허브만 가능
   */
  private static void checkCreatePermission(
      UUID requesterId, UUID hubId, PermissionChecker permissionChecker) {
    if (!permissionChecker.hasCreatePermission(requesterId, hubId, PERMISSION_ROLE)) {
      throw new NoPermissionException(StockErrorCode.STOCK_REGISTER_FORBIDDEN);
    }
  }

  private void checkUpdatePermission(
      UUID requesterId, UUID hubId, PermissionChecker permissionChecker) {
    if (!permissionChecker.hasUpdatePermission(requesterId, hubId, PERMISSION_ROLE)) {
      throw new NoPermissionException(StockErrorCode.STOCK_UPDATE_FORBIDDEN);
    }
  }

  private void checkDeletePermission(
      UUID requesterId, UUID hubId, PermissionChecker permissionChecker) {
    if (!permissionChecker.hasDeletePermission(requesterId, hubId, PERMISSION_ROLE)) {
      throw new NoPermissionException(StockErrorCode.STOCK_DELETED_FORBIDDEN);
    }
  }

  private static void checkHubExistence(UUID hubId, HubExistenceChecker hubExistenceChecker) {
    if (!hubExistenceChecker.hasHub(hubId)) {
      throw new NotFoundHubException();
    }
  }

  private static void checkVendorProductExistence(
      UUID vendorId,
      UUID productId,
      UUID hubId,
      VendorProductExistenceChecker vendorProductExistenceChecker) {
    if (!vendorProductExistenceChecker.hasVendorProduct(vendorId, productId, hubId)) {
      throw new InvalidProductException();
    }
  }

  private void checkValidateDecrease(Integer amount, Type cause) {
    checkValidAmount(amount);
    if (!DECREASE_TYPES.contains(cause)) {
      throw new InvalidStockTypeException(StockErrorCode.STOCK_NOT_DECREASE_TYPE);
    }
    if (this.quantity < amount) {
      throw new InvalidQuantityException(StockErrorCode.STOCK_INSUFFICIENT);
    }
  }

  private void checkValidateIncrease(Integer amount, Type cause) {
    checkValidAmount(amount);
    if (!INCREASE_TYPES.contains(cause)) {
      throw new InvalidStockTypeException(StockErrorCode.STOCK_NOT_INCREASE_TYPE);
    }
  }

  private void checkValidateOrder(
      UUID requesterId, UUID orderId, OrderValidateChecker orderValidateChecker) {
    // 현재 주문과 재고 감소 요청은 동기식으로 동작하므로 추가 유효성 검사는 하지않음
    return;
  }

  private void checkValidateDeleteStock(Type cause) {
    if (!DELETED_TYPES.contains(cause)) {
      throw new InvalidStockTypeException(StockErrorCode.STOCK_NOT_DELETED_TYPE);
    }
    if (this.quantity > 0) {
      throw new InvalidQuantityException(StockErrorCode.STOCK_EXISTS);
    }
  }

  private void checkValidAmount(Integer amount) {
    if (Objects.isNull(amount) || (amount < 0)) {
      throw new InvalidQuantityException();
    }
  }
}
