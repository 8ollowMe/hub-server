package followMe.hub_server.common.config;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.java()));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withCacheConfiguration("hub", defaultConfig.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("hubSearch", defaultConfig.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("hubRoute", defaultConfig.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("hubRouteSearch", defaultConfig.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("hubRoutePath", defaultConfig.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("vendorInfo", defaultConfig.entryTtl(Duration.ofMinutes(10)))
        .build();
  }
}
