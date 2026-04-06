package followMe.hub_server.hub.infrastructure.client.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class VendorClientFeignConfig {

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

  /**
   * @param request 받았던 요청
   * @param requestTemplate 보낼 요청
   * @param headerName 헤더 이름
   */
  private void copyHeader(
      HttpServletRequest request, feign.RequestTemplate requestTemplate, String headerName) {
    String value = request.getHeader(headerName);
    if (StringUtils.hasText(value)) {
      requestTemplate.header(headerName, value);
    }
  }
}
