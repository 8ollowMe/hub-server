package followMe.hub_server.stock.infrastructure.persistence;

import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HubStockRepositoryImpl implements HubStockRepository {
  private final HubStockJpaRepository jpaRepository;

  @Override
  public HubStock save(HubStock hubStock) {
    return jpaRepository.save(hubStock);
  }

  @Override
  public Optional<HubStock> findById(UUID productId) {
    return jpaRepository.findById(productId);
  }

  @Override
  public boolean existsById(UUID productId) {
    return jpaRepository.existsById(productId);
  }

  @Override
  public Collection<HubStock> findAllByProductIdIn(Collection<UUID> productIds) {
    return jpaRepository.findAllByProductIdIn(productIds);
  }

  @Override
  public Page<HubStock> findAllByProductId(UUID productId, Pageable pageable) {
    return jpaRepository.findAllByProductId(productId, pageable);
  }

  @Override
  public Page<HubStock> findAllByHubId(UUID hubId, Pageable pageable) {
    return jpaRepository.findAllByHubId(hubId, pageable);
  }

  @Override
  public Page<HubStock> findAllByVendor_Id(UUID vendorId, Pageable pageable) {
    return jpaRepository.findAllByVendor_Id(vendorId, pageable);
  }

  @Override
  public Page<HubStock> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable);
  }
}
