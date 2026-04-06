package followMe.hub_server.stock.infrastructure.client.checker;

import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import followMe.hub_server.stock.infrastructure.persistence.HubByStockJapRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HubExistenceCheckerImpl implements HubExistenceChecker {
  private final HubByStockJapRepository hubRepository;

  @Override
  public boolean hasHub(UUID hubId) {
    return hubRepository.existsByHubId(hubId);
  }
}
