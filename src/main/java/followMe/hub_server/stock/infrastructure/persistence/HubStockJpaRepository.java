package followMe.hub_server.stock.infrastructure.persistence;

import followMe.hub_server.stock.domain.HubStock;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubStockJpaRepository extends JpaRepository<HubStock, UUID> {
  Collection<HubStock> findAllByProductIdIn(Collection<UUID> productIds);

  Page<HubStock> findAll(Pageable pageable);

  Page<HubStock> findAllByProductId(UUID productId, Pageable pageable);

  Page<HubStock> findAllByHubId(UUID hubId, Pageable pageable);

  Page<HubStock> findAllByVendor_Id(UUID vendorId, Pageable pageable);

  Page<HubStock> findAllByProductIdIn(Collection<UUID> productIds, Pageable pageable);
}
