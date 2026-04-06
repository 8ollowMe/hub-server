package followMe.hub_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@EnableFeignClients
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {"com.followMe", "followMe.hub_server"})
@EntityScan(basePackages = {"com.followMe", "followMe.hub_server"})
@SpringBootApplication(scanBasePackages = {"com.followMe", "followMe.hub_server"})
public class HubApplication {

  public static void main(String[] args) {
    SpringApplication.run(HubApplication.class, args);
  }
}
