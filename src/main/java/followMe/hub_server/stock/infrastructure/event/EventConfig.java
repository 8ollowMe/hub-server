package followMe.hub_server.stock.infrastructure.event;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// TODO: retry 설정 추가해야 됨
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class EventConfig implements AsyncConfigurer {
  private final ApplicationContext ctx;

  @Bean
  public InitializingBean eventsInitializer() {
    return () -> Events.setPublisher(ctx);
  }

  @Nullable
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("AsyncEvent-");
    executor.initialize();

    executor.initialize();
    return executor;
  }
}
