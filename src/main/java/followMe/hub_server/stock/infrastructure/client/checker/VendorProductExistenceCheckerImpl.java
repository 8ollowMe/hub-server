package followMe.hub_server.stock.infrastructure.client.checker;

import followMe.hub_server.stock.domain.service.VendorProductExistenceChecker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VendorProductExistenceCheckerImpl implements VendorProductExistenceChecker {

  //  private final VendorFeignClient vendorFeignClient;

  @Override
  public boolean hasVendorProduct(UUID vendorId, UUID productId, UUID hubId) {
    return true;
  }
}
