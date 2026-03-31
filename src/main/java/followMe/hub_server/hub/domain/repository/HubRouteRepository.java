package followMe.hub_server.hub.domain.repository;

import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    Optional<HubRoute> findByOriginHubAndDestinationHub(Hub originHub, Hub destinationHub);

    Optional<HubRoute> findByOriginHub_HubIdAndDestinationHub_HubId(UUID originHubId, UUID destinationHubId);

    List<HubRoute> findAllByOriginHub(Hub originHub);

    List<HubRoute> findAllByDestinationHub(Hub destinationHub);

    boolean existsByOriginHubAndDestinationHub(Hub originHub, Hub destinationHub);

    boolean existsByOriginHub_HubIdAndDestinationHub_HubId(UUID originHubId, UUID destinationHubId);
}