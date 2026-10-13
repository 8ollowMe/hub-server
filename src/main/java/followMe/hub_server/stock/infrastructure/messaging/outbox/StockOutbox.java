package followMe.hub_server.stock.infrastructure.messaging.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "p_stock_outbox",
    indexes = @Index(name = "idx_stock_outbox_status_created", columnList = "status, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOutbox {

  public enum Status {
    PENDING,
    PUBLISHED
  }

  @Id private UUID id; // 멱등성 키

  @Column(nullable = false)
  private String aggregateKey; // Kafka key = productId (같은 상품은 같은 파티션 → 순서 보존)

  @Column(nullable = false)
  private String topic;

  @Column(nullable = false, columnDefinition = "text")
  private String payload; // StockChangedMessage JSON

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(nullable = false)
  private int retryCount;

  private String lastError;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant publishedAt;

  public static StockOutbox pending(UUID id, String aggregateKey, String topic, String payload) {
    StockOutbox o = new StockOutbox();
    o.id = id;
    o.aggregateKey = aggregateKey;
    o.topic = topic;
    o.payload = payload;
    o.status = Status.PENDING;
    o.retryCount = 0;
    o.createdAt = Instant.now();
    return o;
  }

  public void markPublished() {
    this.status = Status.PUBLISHED;
    this.publishedAt = Instant.now();
  }

  public void markAttemptFailed(String error) {
    this.retryCount++;
    this.lastError = error != null && error.length() > 500 ? error.substring(0, 500) : error;
  }
}
