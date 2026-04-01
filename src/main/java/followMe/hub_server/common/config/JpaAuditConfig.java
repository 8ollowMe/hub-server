package followMe.hub_server.common.config;

import followMe.hub_server.common.audit.AuditorContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class JpaAuditConfig {

  @Bean
  public AuditorAware<UUID> auditorAware() {
    return () -> Optional.ofNullable(AuditorContext.getCurrentUserId());
  }
}
