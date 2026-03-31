package followMe.hub_server.hub.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_hub_route")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_active = true")
public class HubRoute {

    private static final BigDecimal MIN_DURATION = BigDecimal.ZERO;
    private static final BigDecimal MIN_DISTANCE = BigDecimal.ZERO;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hub_route_id", nullable = false, updatable = false)
    private UUID hubRouteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_hub_id", nullable = false)
    private Hub originHub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_hub_id", nullable = false)
    private Hub destinationHub;

    @Column(name = "duration", nullable = false, precision = 10, scale = 2)
    private BigDecimal duration;

    @Column(name = "distance", nullable = false, precision = 10, scale = 2)
    private BigDecimal distance;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder
    public HubRoute(Hub originHub, Hub destinationHub, BigDecimal duration, BigDecimal distance) {
        validate(originHub, destinationHub, duration, distance);

        this.originHub = originHub;
        this.destinationHub = destinationHub;
        this.duration = duration;
        this.distance = distance;
        this.isActive = true;
    }

    public void updateRouteInfo(Hub originHub, Hub destinationHub, BigDecimal duration, BigDecimal distance) {
        validate(originHub, destinationHub, duration, distance);

        this.originHub = originHub;
        this.destinationHub = destinationHub;
        this.duration = duration;
        this.distance = distance;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    private void validate(Hub originHub, Hub destinationHub, BigDecimal duration, BigDecimal distance) {
        validateOriginHub(originHub);
        validateDestinationHub(destinationHub);
        validateSameHub(originHub, destinationHub);
        validateDuration(duration);
        validateDistance(distance);
    }

    private void validateOriginHub(Hub originHub) {
        if (originHub == null) {
            throw new IllegalArgumentException("출발 허브는 필수입니다.");
        }
    }

    private void validateDestinationHub(Hub destinationHub) {
        if (destinationHub == null) {
            throw new IllegalArgumentException("도착 허브는 필수입니다.");
        }
    }

    private void validateSameHub(Hub originHub, Hub destinationHub) {
        if (originHub.getHubId().equals(destinationHub.getHubId())) {
            throw new IllegalArgumentException("출발 허브와 도착 허브는 같을 수 없습니다.");
        }
    }

    private void validateDuration(BigDecimal duration) {
        if (duration == null) {
            throw new IllegalArgumentException("소요 시간은 필수입니다.");
        }

        if (duration.compareTo(MIN_DURATION) <= 0) {
            throw new IllegalArgumentException("소요 시간은 0보다 커야 합니다.");
        }
    }

    private void validateDistance(BigDecimal distance) {
        if (distance == null) {
            throw new IllegalArgumentException("이동 거리는 필수입니다.");
        }

        if (distance.compareTo(MIN_DISTANCE) <= 0) {
            throw new IllegalArgumentException("이동 거리는 0보다 커야 합니다.");
        }
    }
}
