package followMe.hub_server.stock.infrastructure.client;

import feign.FeignException;
import followMe.hub_server.stock.domain.exception.detail.NotFoundProductException;
import followMe.hub_server.stock.domain.exception.detail.VendorClientUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VendorFeignClientFallbackFactory implements FallbackFactory<VendorFeignClient> {

  @Override
  public VendorFeignClient create(Throwable cause) {
    return productId -> {
      // NotFoundProductException - 재시도 X
      if (cause instanceof FeignException.BadRequest) {
        throw new NotFoundProductException();
      }

      log.error("Feign 실패(재시도 가능 에러): {}", cause.getMessage());
      throw new VendorClientUnavailableException();
    };
  }
}
