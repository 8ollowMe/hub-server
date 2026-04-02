package followMe.hub_server.stock.domain;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubStockRepository {

  HubStock save(HubStock hubStock);

  Optional<HubStock> findById(UUID productId);

  boolean existsById(UUID productId);

  Collection<HubStock> findAllByProductIdIn(Collection<UUID> productIds);

  // Query DSL 도입시 분리
  Page<HubStock> findAllByProductId(UUID productId, Pageable pageable);

  Page<HubStock> findAllByHubId(UUID hubId, Pageable pageable);

  Page<HubStock> findAllByVendor_Id(UUID vendorId, Pageable pageable);

  Page<HubStock> findAll(Pageable pageable);

  Page<HubStock> findAllByProductIdIn(Collection<UUID> productIds, Pageable pageable);
}
