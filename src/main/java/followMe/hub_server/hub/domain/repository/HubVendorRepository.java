package followMe.hub_server.hub.domain.repository;

import followMe.hub_server.hub.domain.entity.HubVendor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubVendorRepository extends JpaRepository<HubVendor, UUID> {

  Optional<HubVendor> findByVendorId(UUID vendorId);

  List<HubVendor> findAllByHub_HubId(UUID hubId);

  boolean existsByVendorId(UUID vendorId);
}
