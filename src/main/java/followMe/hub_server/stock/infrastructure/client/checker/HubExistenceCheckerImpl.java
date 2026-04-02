package followMe.hub_server.stock.infrastructure.client.checker;

import followMe.hub_server.stock.domain.service.HubExistenceChecker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HubExistenceCheckerImpl implements HubExistenceChecker {

  //  private final HubFeignClient hubClient;

  @Override
  public boolean hasHub(UUID hubId) {
    return true;
  }
}
