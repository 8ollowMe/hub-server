package followMe.hub_server.common.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditorFilter extends OncePerRequestFilter {

  private static final String USER_ID_HEADER = "X-User-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String userIdHeader = request.getHeader(USER_ID_HEADER);

      if (StringUtils.hasText(userIdHeader)) {
        AuditorContext.setCurrentUserId(UUID.fromString(userIdHeader));
      }

      filterChain.doFilter(request, response);
    } finally {
      AuditorContext.clear();
    }
  }
}
