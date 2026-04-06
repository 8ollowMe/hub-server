package followMe.hub_server.stock.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HubDto {
  private UUID id;
  private String name;
  private String address;
  private BigDecimal latitude;
  private BigDecimal longitude;
}
