package followMe.hub_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
<<<<<<< HEAD
import org.springframework.boot.autoconfigure.domain.EntityScan;
=======
import org.springframework.cache.annotation.EnableCaching;
>>>>>>> a94c7eb9e7bc1f238d2d56f6b6b90a87ea516ef3
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

<<<<<<< HEAD
@EnableScheduling
@EnableFeignClients
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {"com.followMe", "followMe.hub_server"})
@EntityScan(basePackages = {"com.followMe", "followMe.hub_server"})
@SpringBootApplication(scanBasePackages = {"com.followMe", "followMe.hub_server"})
=======
@EnableCaching
@EnableJpaAuditing
@EnableFeignClients
@SpringBootApplication
>>>>>>> a94c7eb9e7bc1f238d2d56f6b6b90a87ea516ef3
public class HubApplication {

  public static void main(String[] args) {
    SpringApplication.run(HubApplication.class, args);
  }
}
