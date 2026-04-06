package followMe.hub_server.stock.infrastructure.client;

import static followMe.hub_server.stock.infrastructure.client.UserFeignClient.USER_PREFIX;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.detail.UserClientUnavailableException;
import followMe.hub_server.stock.infrastructure.client.dto.UserDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 유저 서버와 통신하는 클라이언트
 *
 * <ul>
 *   현재 설정
 *   <li>{@link feign.FeignException.BadRequest},{@link feign.FeignException.NotFound} : 재시도 X
 *   <li>그 외 응답일 경우 응답을 {@link UserFeignClientFallbackFactory} 통해 {@link
 *       UserClientUnavailableException} 로 변환
 *   <li>{@link UserClientUnavailableException} 일 경우, 재시도 가능한 응답으로 간주(일시적 장애로 판단) '유저 서비스 통신에 문제가
 *       생겼습니다.' 안내 (3회 retry)
 *       <ul>
 *         <li>설정 된 서킷브레이커 초과 시 -> OPEN 으로 전환
 *       </ul>
 *   <li>OPEN 상태일때는 요청을 밖으로 보내지않고, '유저 서비스 상태가 불안하여, 요청이 일시 차단 되었습니다.' 안내
 *       <ul>
 */
@Component
@FeignClient(
    name = "user-server",
    path = USER_PREFIX,
    configuration = FeignClientConfig.class,
    fallbackFactory = UserFeignClientFallbackFactory.class)
public interface UserFeignClient {
  String USER_PREFIX = "/internal/v1/users";

  @CircuitBreaker(name = "user-server", fallbackMethod = "handleCircuitOpen")
  @Retry(name = "user-server")
  @GetMapping("/{userId}")
  UserDto getUser(@PathVariable UUID userId);

  /*
   * 서킷브레이커 OPEN 상태일 때 처리
   *
   * CallNotPermittedException = 서킷 브레이커 OPEN 상태의 예외
   * 요청을 차단하고,
   * '유저 서비스 상태가 불안하여, 요청이 일시 차단 되었습니다.' 안내 메시지 출력
   */
  default UserDto handleCircuitOpen(UUID userId, CallNotPermittedException e) {
    throw new UserClientUnavailableException(StockErrorCode.USER_CLIENT_CIRCUIT_BREAKER);
  }
}
