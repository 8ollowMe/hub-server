package followMe.hub_server.stock.infrastructure.persistence;

import followMe.hub_server.stock.domain.HubStockHistory;
import followMe.hub_server.stock.domain.HubStockHistoryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubStockHistoryRepositoryImpl
    extends JpaRepository<HubStockHistory, UUID>, HubStockHistoryRepository {}
