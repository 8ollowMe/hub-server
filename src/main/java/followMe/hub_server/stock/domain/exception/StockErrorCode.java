package followMe.hub_server.stock.domain.exception;

import com.followMe.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StockErrorCode implements ErrorCode {
  STOCK_INVALID_QUANTITY("STOCK_002", "요청한 수량이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  STOCK_INVALID_INIT_QUANTITY("STOCK_003", "초기 물품 수량은 0보다 크거나 같아야 합니다.", HttpStatus.BAD_REQUEST),
  STOCK_INSUFFICIENT("STOCK_004", "재고가 충분하지 않습니다.", HttpStatus.CONFLICT),
  STOCK_NOT_DECREASE_TYPE("STOCK_005", "해당 타입은 재고 감소 유형이 이닙니다.", HttpStatus.BAD_REQUEST),
  STOCK_NOT_INCREASE_TYPE("STOCK_006", "해당 타입은 재고 증가 유형이 아닙니다.", HttpStatus.BAD_REQUEST),
  STOCK_NOT_DELETED_TYPE("STOCK_007", "해당 타입은 재고 삭제 유형이 아닙니다.", HttpStatus.BAD_REQUEST),
  STOCK_EXISTS("STOCK_008", "재고가 남아있어 요청을 처리할 수 없습니다.", HttpStatus.BAD_REQUEST),
  STOCK_TYPE_NULL("STOCK_008", "요청 재고 타입이 비어있습니다.", HttpStatus.BAD_REQUEST),
  STOCK_INVALID_SEARCH_CONDITION("STOCK_009", "지원하지 않는 재고 검색 조건 입니다.", HttpStatus.BAD_REQUEST),

  PRODUCT_NOT_FOUND("STOCK_010", "존재하지 않는 상품 입니다.", HttpStatus.NOT_FOUND),
  PRODUCT_INVALID_INFO("STOCK_011", "존재하지 않거나, 허브에서 관리하는 상품이 아닙니다.", HttpStatus.BAD_REQUEST),
  PRODUCT_DUPLICATED("STOCK_012", "이미 해당 상품이 재고로 등록되어 있습니다.", HttpStatus.BAD_REQUEST),

  HUB_NOT_FOUND("STOCK_020", "존재하지 않는 허브 입니다.", HttpStatus.NOT_FOUND),

  ORDER_DUPLICATED_PRODUCT("STOCK_030", "중복된 상품에 대한 재고 수정 요청이 존재합니다.", HttpStatus.BAD_REQUEST),

  USER_NOT_FOUND("STOCK_040", "존재하지 않는 사용자 입니다.", HttpStatus.NOT_FOUND),

  VENDOR_CLIENT_UNAVAILABLE("STOCK_050", "업체 서비스 통신에 문제가 생겼습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  VENDOR_CLIENT_CIRCUIT_BREAKER(
      "STOCK_050", "업체 서비스 상태가 불안하여, 요청이 일시 차단 되었습니다.", HttpStatus.SERVICE_UNAVAILABLE),

  USER_CLIENT_UNAVAILABLE("STOCK_060", "유저 서비스 통신에 문제가 생겼습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  USER_CLIENT_CIRCUIT_BREAKER(
      "STOCK_061", "유저 서비스 상태가 불안하여, 요청이 일시 차단 되었습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  USER_INVALID_INFO("STOCK_062", "잘못된 유저 정보 요청입니다.", HttpStatus.BAD_REQUEST),

  STOCK_REGISTER_FORBIDDEN("STOCK_100", "재고를 등록할 권한이 부족합니다.", HttpStatus.FORBIDDEN),
  STOCK_UPDATE_FORBIDDEN("STOCK_101", "재고를 수정할 권한이 부족합니다.", HttpStatus.FORBIDDEN),
  STOCK_DELETED_FORBIDDEN("STOCK_102", "재고를 삭제할 권한이 부족합니다.", HttpStatus.FORBIDDEN),

  UNKNOWN_SERVER_CONNECT_FAIL(
      "STOCK_110", "다른 서버와의 통신 중 알수없는 연결 오류 입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
