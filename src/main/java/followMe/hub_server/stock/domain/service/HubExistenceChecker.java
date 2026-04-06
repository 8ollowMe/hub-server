package followMe.hub_server.stock.domain.service;

import java.util.UUID;

public interface HubExistenceChecker {
  boolean hasHub(UUID hubId);
}
