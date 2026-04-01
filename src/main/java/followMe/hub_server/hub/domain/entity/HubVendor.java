package followMe.hub_server.hub.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "p_hub_vendor")
@SQLRestriction("is_active = true")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class HubVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hub_vendor_id", nullable = false, updatable = false)
    private UUID hubVendorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Column(name = "vendor_id", nullable = false, unique = true)
    private UUID vendorId;

    @Column(name = "vendor_name", length = 100)
    private String vendorName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder
    public HubVendor(Hub hub, UUID vendorId, String vendorName) {
        this.hub = Objects.requireNonNull(hub);
        this.vendorId = Objects.requireNonNull(vendorId);
        this.vendorName = vendorName;
        this.isActive = true;
    }

    public void active() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
