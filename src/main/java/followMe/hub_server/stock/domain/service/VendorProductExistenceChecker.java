package followMe.hub_server.stock.domain.service;

import java.util.UUID;

public interface VendorProductExistenceChecker {
  boolean hasVendorProduct(UUID vendorId, UUID productId, UUID hubId);
}
