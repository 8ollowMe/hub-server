package followMe.hub_server.common.audit;

import java.util.UUID;

public final class AuditorContext {

  private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();

  private AuditorContext() {}

  public static void setCurrentUserId(UUID userId) {
    CURRENT_USER_ID.set(userId);
  }

  public static UUID getCurrentUserId() {
    return CURRENT_USER_ID.get();
  }

  public static void clear() {
    CURRENT_USER_ID.remove();
  }
}
