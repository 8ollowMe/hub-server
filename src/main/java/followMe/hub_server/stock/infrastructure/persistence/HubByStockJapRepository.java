package followMe.hub_server.stock.infrastructure.persistence;

import followMe.hub_server.hub.domain.entity.Hub;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * 재고 등록을 위해 사용
 * hub 존재유무 파악
 */
public interface HubByStockJapRepository extends JpaRepository<Hub, UUID> {
  boolean existsByHubId(UUID hubId);
}
