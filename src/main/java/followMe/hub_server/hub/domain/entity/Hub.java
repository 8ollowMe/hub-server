package followMe.hub_server.hub.domain.entity;

import com.followMe.common.entity.BaseAudit;
import followMe.hub_server.hub.exception.detail.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_hub")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Hub extends BaseAudit {

	private static final int HUB_NAME_MAX_LENGTH = 50;
	private static final int ADDRESS_MAX_LENGTH = 255;
	private static final int DELETED_BY_MAX_LENGTH = 100;

	private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90.0000000");
	private static final BigDecimal MAX_LATITUDE = new BigDecimal("90.0000000");
	private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180.0000000");
	private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180.0000000");

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "hub_id", nullable = false, updatable = false)
	private UUID hubId;

	@Column(name = "hub_name", nullable = false, length = HUB_NAME_MAX_LENGTH)
	private String hubName;

	@Column(name = "address", nullable = false, length = ADDRESS_MAX_LENGTH)
	private String address;

	@Column(name = "latitude", nullable = false, precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(name = "longitude", nullable = false, precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "deleted_by", length = DELETED_BY_MAX_LENGTH)
	private String deletedBy;

	@Builder
	public Hub(String hubName, String address, BigDecimal latitude, BigDecimal longitude) {
		validate(hubName, address, latitude, longitude);

		this.hubName = normalize(hubName);
		this.address = normalize(address);
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void update(String hubName, String address, BigDecimal latitude, BigDecimal longitude) {
		validate(hubName, address, latitude, longitude);

		this.hubName = normalize(hubName);
		this.address = normalize(address);
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void softDelete(String deletedBy) {
		validateDeletedBy(deletedBy);

		this.deletedAt = LocalDateTime.now();
		this.deletedBy = deletedBy.trim();
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	private void validate(String hubName, String address, BigDecimal latitude, BigDecimal longitude) {
		if (isDeleted()) {
			throw new HubAlreadyDeletedException();
		}

		validateHubName(hubName);
		validateAddress(address);
		validateLatitude(latitude);
		validateLongitude(longitude);
	}

	private void validateHubName(String hubName) {
		if (hubName == null || hubName.isBlank()) {
			throw new InvalidHubNameException();
		}

		String normalizedHubName = hubName.trim();

		if (normalizedHubName.length() > HUB_NAME_MAX_LENGTH) {
			throw new InvalidHubNameException("허브명은 " + HUB_NAME_MAX_LENGTH + "자를 초과할 수 없습니다.");
		}
	}

	private void validateAddress(String address) {
		if (address == null || address.isBlank()) {
			throw new InvalidAddressException();
		}

		String normalizedAddress = address.trim();

		if (normalizedAddress.length() > ADDRESS_MAX_LENGTH) {
			throw new InvalidAddressException("주소는 " + ADDRESS_MAX_LENGTH + "자를 초과할 수 없습니다.");
		}
	}

	private void validateLatitude(BigDecimal latitude) {
		if (latitude == null) {
			throw new InvalidLatitudeException();
		}

		if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
			throw new InvalidLatitudeException("위도는 " + MIN_LATITUDE + " 이상 " + MAX_LATITUDE + " 이하여야 합니다.");
		}
	}

	private void validateLongitude(BigDecimal longitude) {
		if (longitude == null) {
			throw new InvalidLongitudeException();
		}

		if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
			throw new InvalidLongitudeException("경도는 " + MIN_LONGITUDE + " 이상 " + MAX_LONGITUDE + " 이하여야 합니다.");
		}
	}

	private void validateDeletedBy(String deletedBy) {
		if (isDeleted()) {
			throw new HubAlreadyDeletedException();
		}

		if (deletedBy == null || deletedBy.isBlank()) {
			throw new InvalidDeletedByException("삭제자는 필수입니다.");
		}

		if (deletedBy.trim().length() > DELETED_BY_MAX_LENGTH) {
			throw new InvalidDeletedByException("삭제자 정보는 " + DELETED_BY_MAX_LENGTH + "자를 초과할 수 없습니다.");
		}
	}

	private String normalize(String value) {
		return value.trim();
	}
}
