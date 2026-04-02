package followMe.hub_server.stock.infrastructure.client.checker;

import followMe.hub_server.stock.domain.service.PermissionChecker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionCheckerImpl implements PermissionChecker {

  //  private final UserFeignClient userFeignClient;

  @Override
  public boolean hasCreatePermission(UUID requesterId, UUID hubId) {
    return true;
  }

  @Override
  public boolean hasUpdatePermission(UUID requesterId, UUID hubId) {
    return true;
  }

  @Override
  public boolean hasDeletePermission(UUID requesterId, UUID hubId) {
    return true;
  }
}
