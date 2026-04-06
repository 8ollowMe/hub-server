package followMe.hub_server.stock.infrastructure.client.checker;

import followMe.hub_server.stock.application.service.UserRole;
import followMe.hub_server.stock.domain.exception.detail.NotFoundUserException;
import followMe.hub_server.stock.domain.service.PermissionChecker;
import followMe.hub_server.stock.infrastructure.client.UserFeignClient;
import followMe.hub_server.stock.infrastructure.client.dto.UserDto.UserInfo;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionCheckerImpl implements PermissionChecker {

  private final UserFeignClient userFeignClient;

  @Override
  public boolean hasCreatePermission(UUID requesterId, UUID hubId, Set<UserRole> permissionRole) {
    UserInfo user = getUserInfo(requesterId);

    if (Objects.isNull(user)) {
      throw new NotFoundUserException();
    }

    if (user.getRole().equals(UserRole.HUB)) {
      return isMatchedHubManager(user, hubId);
    }
    return permissionRole.contains(user.getRole());
  }

  @Override
  public boolean hasUpdatePermission(UUID requesterId, UUID hubId, Set<UserRole> permissionRole) {
    UserInfo user = getUserInfo(requesterId);

    if (Objects.isNull(user)) {
      throw new NotFoundUserException();
    }

    if (user.getRole().equals(UserRole.HUB)) {
      return isMatchedHubManager(user, hubId);
    }
    return permissionRole.contains(user.getRole());
  }

  @Override
  public boolean hasDeletePermission(UUID requesterId, UUID hubId, Set<UserRole> permissionRole) {
    UserInfo user = getUserInfo(requesterId);

    if (Objects.isNull(user)) {
      throw new NotFoundUserException();
    }

    if (user.getRole().equals(UserRole.HUB)) {
      return isMatchedHubManager(user, hubId);
    }
    return permissionRole.contains(user.getRole());
  }

  private UserInfo getUserInfo(UUID userId) {
    return userFeignClient.getUser(userId).getData();
  }

  private boolean isMatchedHubManager(UserInfo user, UUID hubId) {
    return user.getHubId().equals(hubId);
  }
}
