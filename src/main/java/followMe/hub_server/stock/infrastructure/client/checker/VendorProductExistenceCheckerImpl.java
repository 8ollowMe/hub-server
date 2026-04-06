package followMe.hub_server.stock.infrastructure.client.checker;

import followMe.hub_server.stock.domain.exception.detail.NotFoundProductException;
import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import followMe.hub_server.stock.infrastructure.client.VendorFeignClient;
import followMe.hub_server.stock.infrastructure.client.dto.VendorProductDto.VendorProductInfo;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VendorProductExistenceCheckerImpl implements VendorProductExistenceChecker {

  private final VendorFeignClient vendorFeignClient;

  @Override
  public boolean hasVendorProduct(UUID vendorId, UUID productId, UUID hubId) {

    VendorProductInfo info = vendorFeignClient.getVendorProduct(productId);

    if (Objects.isNull(info)) {
      throw new NotFoundProductException();
    }
    return info.equals(productId, hubId, vendorId);
  }
}
