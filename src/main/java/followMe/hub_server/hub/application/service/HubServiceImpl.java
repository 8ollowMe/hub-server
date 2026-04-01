package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.command.CreateHubCommand;
import followMe.hub_server.hub.application.dto.command.GetHubsQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubCommand;
import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import followMe.hub_server.hub.application.dto.result.HubResult;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.detail.HubNotFoundException;
import followMe.hub_server.hub.exception.detail.InvalidAuthException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubServiceImpl implements HubService {

  private final HubRepository hubRepository;

  @Override
  @Transactional
  public HubResult createHub(UserContext userContext, CreateHubCommand command) {
    validateAuthForCreate(userContext);

    Hub hub =
        Hub.builder()
            .hubName(command.name())
            .address(command.address())
            .latitude(command.latitude())
            .longitude(command.longitude())
            .build();

    Hub savedHub = hubRepository.save(hub);
    return HubResult.from(savedHub);
  }

  @Override
  public HubResult getHub(UUID hubId) {
    Hub hub = hubRepository.findById(hubId).orElseThrow(HubNotFoundException::new);

    return HubResult.from(hub);
  }

  @Override
  public GetHubsPageResult searchHubs(GetHubsQuery query) {

    Page<Hub> page = hubRepository.searchByKeyword(query.keyword(), query.pageRequest().toPageable());

    return GetHubsPageResult.from(page);
  }

  @Override
  @Transactional
  public HubResult updateHub(UserContext userContext, UpdateHubCommand command) {
    validateAuth(userContext, command.hubId());

    Hub hub = hubRepository.findById(command.hubId()).orElseThrow(HubNotFoundException::new);

    hub.update(command.name(), command.address(), command.latitude(), command.longitude());

    return HubResult.from(hub);
  }

  @Override
  @Transactional
  public HubResult deleteHub(UserContext userContext, UUID hubId) {
    validateAuth(userContext, hubId);

    Hub hub = hubRepository.findById(hubId).orElseThrow(HubNotFoundException::new);

    hub.softDelete(userContext.userId());

    return HubResult.from(hub);
  }

  private void validateAuth(UserContext userContext, UUID targetHubId) {
    if (UserRole.MASTER.equals(userContext.role())) {
      return;
    }

    if (UserRole.HUB_MANAGER.equals(userContext.role())
        && userContext.hubId() != null
        && userContext.hubId().equals(targetHubId)) {
      return;
    }

    throw new InvalidAuthException(HubErrorCode.INVALID_AUTH);
  }

  private void validateAuthForCreate(UserContext userContext) {
    if (UserRole.MASTER.equals(userContext.role())) {
      return;
    }

    throw new InvalidAuthException(HubErrorCode.INVALID_AUTH);
  }
}
