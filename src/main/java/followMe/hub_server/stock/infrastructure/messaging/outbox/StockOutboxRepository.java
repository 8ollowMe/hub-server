package followMe.hub_server.stock.infrastructure.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockOutboxRepository extends JpaRepository<StockOutbox, UUID> {
  List<StockOutbox> findTop50ByStatusOrderByCreatedAtAsc(StockOutbox.Status status);

  List<StockOutbox> findTop50ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
      StockOutbox.Status status, Instant time);
}
