package followMe.hub_server.hub.domain.repository;


import followMe.hub_server.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    Optional<Hub> findByHubId(UUID hubId);

    boolean existsByHubId(UUID hubId);
}
