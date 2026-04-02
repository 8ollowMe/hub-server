package followMe.hub_server.stock.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubStockHistoryRepository {
  HubStockHistory save(HubStockHistory history);

  Page<HubStockHistory> findByProductId(UUID productId, Pageable pageable);
}
