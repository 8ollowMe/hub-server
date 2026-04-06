package followMe.hub_server.hub.exception;

import com.followMe.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HubErrorCode implements ErrorCode {
  HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_001", "허브를 찾을 수 없습니다."),
  HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_002", "허브 경로를 찾을 수 없습니다."),
  HUB_ROUTE_ALREADY_EXISTS(HttpStatus.CONFLICT, "HUB_003", "이미 존재하는 허브 경로입니다."),
  SAME_ORIGIN_DESTINATION(HttpStatus.BAD_REQUEST, "HUB_004", "출발 허브와 도착 허브는 같을 수 없습니다."),
  INVALID_HUB_NAME(HttpStatus.BAD_REQUEST, "HUB_005", "허브명이 올바르지 않습니다."),
  INVALID_ADDRESS(HttpStatus.BAD_REQUEST, "HUB_006", "주소가 올바르지 않습니다."),
  INVALID_LATITUDE(HttpStatus.BAD_REQUEST, "HUB_007", "위도 값이 올바르지 않습니다."),
  INVALID_LONGITUDE(HttpStatus.BAD_REQUEST, "HUB_008", "경도 값이 올바르지 않습니다."),
  INVALID_DURATION(HttpStatus.BAD_REQUEST, "HUB_009", "소요 시간은 0보다 커야 합니다."),
  INVALID_DISTANCE(HttpStatus.BAD_REQUEST, "HUB_010", "이동 거리는 0보다 커야 합니다."),
  HUB_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "HUB_011", "이미 삭제된 허브입니다."),
  HUB_ROUTE_ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, "HUB_012", "이미 비활성화된 허브 경로입니다."),
  INVALID_DELETED_BY(HttpStatus.BAD_REQUEST, "HUB_013", "삭제자 정보가 올바르지 않습니다."),
  ORIGIN_HUB_REQUIRED(HttpStatus.BAD_REQUEST, "HUB_014", "출발 허브는 필수입니다."),
  DESTINATION_HUB_REQUIRED(HttpStatus.BAD_REQUEST, "HUB_015", "도착 허브는 필수입니다."),
  INVALID_AUTH(HttpStatus.UNAUTHORIZED, "HUB_016", "권한이 없습니다."),
  VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_017", "업체 정보를 찾을 수 없습니다."),
  INVALID_VENDOR_RESPONSE(HttpStatus.BAD_REQUEST, "HUB_018", "업체에 소속 허브 정보가 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
