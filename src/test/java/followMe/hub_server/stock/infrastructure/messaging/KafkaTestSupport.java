package followMe.hub_server.stock.infrastructure.messaging;

import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

final class KafkaTestSupport {
  private KafkaTestSupport() {}

  static List<ConsumerRecord<String, String>> drain(
      String bootstrap, String topic, String key, Duration timeout) {
    Properties p = new Properties();
    p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    p.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
    p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    List<ConsumerRecord<String, String>> found = new ArrayList<>();
    long deadline = System.nanoTime() + timeout.toNanos();
    try (KafkaConsumer<String, String> c = new KafkaConsumer<>(p)) {
      c.subscribe(List.of(topic));
      while (System.nanoTime() < deadline) {
        for (ConsumerRecord<String, String> r : c.poll(Duration.ofMillis(500))) {
          if (key.equals(r.key())) found.add(r);
        }
      }
    }
    return found;
  }
}
