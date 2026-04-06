package followMe.hub_server.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI hubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hub Server API")
                        .version("v1")
                        .description("허브 서버 API 문서"));
    }
}