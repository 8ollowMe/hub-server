package followMe.hub_server.hub.domain.entity;

import com.followMe.common.entity.BaseAudit;
import followMe.hub_server.hub.exception.detail.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "p_hub_route")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class HubRoute extends BaseAudit {

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

  @Builder
  public HubRoute(Hub originHub, Hub destinationHub, BigDecimal duration, BigDecimal distance) {
    validate(originHub, destinationHub, duration, distance);

    this.originHub = originHub;
    this.destinationHub = destinationHub;
    this.duration = duration;
    this.distance = distance;
  }

  public void update(
      Hub originHub, Hub destinationHub, BigDecimal duration, BigDecimal distance) {
    validate(originHub, destinationHub, duration, distance);

    this.originHub = originHub;
    this.destinationHub = destinationHub;
    this.duration = duration;
    this.distance = distance;
  }

  public void softDeleteRoute(UUID deletedBy) {
    if (this.isDeleted())
        throw new HubRouteAlreadyInactiveException();
    super.softDelete(deletedBy);
  }

  private void validate(
      Hub originHub, Hub destinationHub, BigDecimal duration, BigDecimal distance) {
    validateOriginHub(originHub);
    validateDestinationHub(destinationHub);
    validateSameHub(originHub, destinationHub);
    validateDuration(duration);
    validateDistance(distance);
  }

  private void validateOriginHub(Hub originHub) {
    if (originHub == null) {
      throw new OriginHubRequiredException();
    }
  }

  private void validateDestinationHub(Hub destinationHub) {
    if (destinationHub == null) {
      throw new DestinationHubRequiredException();
    }
  }

  private void validateSameHub(Hub originHub, Hub destinationHub) {
    if (originHub.getHubId().equals(destinationHub.getHubId())) {
      throw new SameOriginDestinationException();
    }
  }

  private void validateDuration(BigDecimal duration) {
    if (duration == null) {
      throw new InvalidDurationException();
    }

    if (duration.compareTo(MIN_DURATION) <= 0) {
      throw new InvalidDurationException("소요 시간은 0보다 커야 합니다.");
    }
  }

  private void validateDistance(BigDecimal distance) {
    if (distance == null) {
      throw new InvalidDistanceException();
    }

    if (distance.compareTo(MIN_DISTANCE) <= 0) {
      throw new InvalidDistanceException("이동 거리는 0보다 커야 합니다.");
    }
  }
}
