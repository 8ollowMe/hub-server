package followMe.hub_server.hub.domain.repository;

import followMe.hub_server.hub.domain.entity.Hub;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HubRepository extends JpaRepository<Hub, UUID> {

  Optional<Hub> findByHubId(UUID hubId);

  boolean existsByHubId(UUID hubId);

  @Query(
      """
            SELECT h
            FROM Hub h
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(h.hubName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(h.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
  Page<Hub> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
