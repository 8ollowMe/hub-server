package followMe.hub_server.stock.infrastructure.client;

import static followMe.hub_server.stock.infrastructure.client.VendorFeignClient.VENDOR_PREFIX;

import followMe.hub_server.stock.domain.exception.StockErrorCode;
import followMe.hub_server.stock.domain.exception.StockException;
import followMe.hub_server.stock.domain.exception.detail.VendorClientUnavailableException;
import followMe.hub_server.stock.infrastructure.client.dto.VendorProductDto.VendorProductInfo;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
 * 업체 서버와 통신하는 클라이언트
 *
 * 현재 설정
 * - 실패 시 retry (3회)
 *   - BadRequest 응답 값일 경우 재시도 X
 *   - 그 외 응답일 경우 '업체 서비스 통신에 문제가 생겼습니다.' 안내 (3회 retry)
 * - 설정 된 서킷브레이커 초과 시 -> OPEN 으로 전환
 *   - OPEN 상태일때는 요청을 밖으로 보내지않고,
 *     '업체 서비스 상태가 불안하여, 요청이 일시 차단 되었습니다.' 안내
 *
 */
@Component
@FeignClient(
    name = "vendor-server",
    path = VENDOR_PREFIX,
    fallbackFactory = VendorFeignClientFallbackFactory.class)
public interface VendorFeignClient {
  static final String VENDOR_PREFIX = "/internal/v1/products";

  @CircuitBreaker(name = "vendor-server", fallbackMethod = "fallback")
  @Retry(name = "vendor-server")
  @GetMapping("/{productId}")
  VendorProductInfo getVendorProduct(@PathVariable UUID productId);

  /*
   * TODO: 에러 디코더 클래스로 분리
   */
  default VendorProductInfo fallback(UUID productId, Throwable t) {
    /*
     * 서킷 브레이커 동작 시 요청 차단 안내를 위한 예외
     * 메시지 내용: '업체 서비스 상태가 불안하여, 요청이 일시 차단 되었습니다.'
     */
    if (t instanceof CallNotPermittedException) {
      throw new VendorClientUnavailableException(StockErrorCode.VENDOR_CLIENT_CIRCUIT_BREAKER);
    }

    /*
     * FallbackFactory 를 거쳐서 나온 에러는 그대로 던지기
     * ex) 상품 존재x 에러, 업체 서버 사용불가 에러
     */

    if (t instanceof StockException re) {
      throw re;
    }

    // 그 외 경우는 RuntimeException
    throw new RuntimeException(t);
  }
}
