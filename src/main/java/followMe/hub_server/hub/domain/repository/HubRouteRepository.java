package followMe.hub_server.hub.domain.repository;

import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

  Optional<HubRoute> findByOriginHubAndDestinationHub(Hub originHub, Hub destinationHub);

  Optional<HubRoute> findByOriginHub_HubIdAndDestinationHub_HubId(
      UUID originHubId, UUID destinationHubId);

  List<HubRoute> findAllByOriginHub(Hub originHub);

  List<HubRoute> findAllByDestinationHub(Hub destinationHub);

    List<HubRoute> findAllByOriginHubOrDestinationHub(Hub originHub, Hub destinationHub);

    Optional<HubRoute> findByHubRouteId(UUID hubRouteId);

  boolean existsByOriginHubAndDestinationHub(Hub originHub, Hub destinationHub);

  boolean existsByOriginHub_HubIdAndDestinationHub_HubId(UUID originHubId, UUID destinationHubId);

    @Query("""
      SELECT hr
      FROM HubRoute hr
      WHERE (:originHubId IS NULL OR hr.originHub.hubId = :originHubId)
        AND (:destinationHubId IS NULL OR hr.destinationHub.hubId = :destinationHubId)
      """)
    Page<HubRoute> search(
            @Param("originHubId") UUID originHubId,
            @Param("destinationHubId") UUID destinationHubId,
            Pageable pageable);
}
