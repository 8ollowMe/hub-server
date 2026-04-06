package followMe.hub_server.stock.infrastructure.client;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignClientConfig {
  private static final String AUTHORIZATION = "Authorization";

  @Bean
  public RequestInterceptor authorizationHeaderForwardInterceptor() {
    return requestTemplate -> {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes == null) {
        return;
      }

      HttpServletRequest request = attributes.getRequest();
      copyHeader(request, requestTemplate, "X-User-Id");
      copyHeader(request, requestTemplate, "X-Role");
      copyHeader(request, requestTemplate, "X-Hub-Id");
      copyHeader(request, requestTemplate, "X-Vendor-Id");
    };
  }

  private void copyHeader(
      HttpServletRequest request, feign.RequestTemplate requestTemplate, String headerName) {
    String value = request.getHeader(headerName);
    if (StringUtils.hasText(value)) {
      requestTemplate.header(headerName, value);
    }
  }
}
