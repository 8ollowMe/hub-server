package followMe.hub_server.stock.domain.service;

import followMe.hub_server.stock.application.service.UserRole;
import java.util.Set;
import java.util.UUID;

public interface PermissionChecker {
  boolean hasCreatePermission(UUID requesterId, UUID hubId, Set<UserRole> permissionRole);

  boolean hasUpdatePermission(UUID requesterId, UUID hubId, Set<UserRole> permissionRole);

  boolean hasDeletePermission(UUID requesterId, UUID hubId, Set<UserRole> permissionRole);
}
