package followMe.hub_server.stock.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor {
  @Column(name = "vendor_id", nullable = false)
  private UUID id;

  @Column(name = "vendor_name", nullable = false)
  private String name;

  public static Vendor of(UUID vendorId, String vendorName) {
    return new Vendor(vendorId, vendorName);
  }
}
