package followMe.hub_server.stock.domain.service;

import java.util.UUID;

public interface PermissionChecker {
  boolean hasCreatePermission(UUID requesterId, UUID hubId);

  boolean hasUpdatePermission(UUID requesterId, UUID hubId);

  boolean hasDeletePermission(UUID requesterId, UUID hubId);
}
