package followMe.hub_server.stock.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VendorFeignClientFallbackFactory implements FallbackFactory<VendorFeignClient> {

  @Override
  public VendorFeignClient create(Throwable cause) {
    return productId -> {
      //            // 재시도 X
      //            if (cause instanceof FeignException.NotFound) {
      //              throw new NotFoundProductException();
      //            }
      //            // 재시도 X
      //            if (cause instanceof FeignException.BadRequest) {
      //              throw new InvalidProductException();
      //            }
      //            log.error("Feign 실패(재시도 가능 에러): {}", cause.getMessage());
      //            throw new VendorClientUnavailableException();
      throw new RuntimeException(cause.getMessage(), cause);
    };
  }
}
