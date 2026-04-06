package followMe.hub_server.stock.infrastructure.client;

import feign.FeignException;
import followMe.hub_server.stock.domain.exception.detail.InvalidUserException;
import followMe.hub_server.stock.domain.exception.detail.NotFoundUserException;
import followMe.hub_server.stock.domain.exception.detail.UserClientUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

  @Override
  public UserFeignClient create(Throwable cause) {
    return userId -> {
      // 재시도 X
      if (cause instanceof FeignException.NotFound) {
        throw new NotFoundUserException();
      }
      // 재시도 X
      if (cause instanceof FeignException.BadRequest) {
        throw new InvalidUserException();
      }

      log.error("Feign 실패(재시도 가능 에러): {}", cause.getMessage());
      throw new UserClientUnavailableException();
    };
  }
}
