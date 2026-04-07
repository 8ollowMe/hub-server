package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.command.CreateHubCommand;
import followMe.hub_server.hub.application.dto.command.GetHubsQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubCommand;
import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import followMe.hub_server.hub.application.dto.result.HubResult;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.domain.repository.HubRouteRepository;
import followMe.hub_server.hub.exception.HubErrorCode;
import followMe.hub_server.hub.exception.detail.HubNotFoundException;
import followMe.hub_server.hub.exception.detail.InvalidAuthException;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubServiceImpl implements HubService {

  private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final HubRoutePathQueryService hubRoutePathQueryService;

  @Override
  @Transactional
  @CacheEvict(value = {"hub", "hubSearch", "hubRoutePath"}, allEntries = true)
  public HubResult createHub(UserContext userContext, CreateHubCommand command) {
    validateAuth(userContext);

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
  @Cacheable(value = "hub", key = "#hubId")
  public HubResult getHub(UUID hubId) {
      log.info("===== getHub 실행됨: " + hubId + " =====");
    Hub hub = hubRepository.findById(hubId).orElseThrow(HubNotFoundException::new);

    return HubResult.from(hub);
  }

  @Override
  @Cacheable(
          value = "hubSearch",
          key = "#query.keyword() + ':' + #query.pageRequest().page + ':' + #query.pageRequest().size"
  )
  public GetHubsPageResult searchHubs(GetHubsQuery query) {
    Page<Hub> page =
        hubRepository.searchByKeyword(query.keyword(), query.pageRequest().toPageable());

    return GetHubsPageResult.from(page);
  }

  @Override
  @Transactional
  @CacheEvict(value = {"hub", "hubSearch", "hubRoutePath"}, allEntries = true)
  public HubResult updateHub(UserContext userContext, UpdateHubCommand command) {
    validateAuth(userContext);

    Hub hub = hubRepository.findById(command.hubId()).orElseThrow(HubNotFoundException::new);

    hub.update(command.name(), command.address(), command.latitude(), command.longitude());

    return HubResult.from(hub);
  }

  @Override
  @Transactional
  @CacheEvict(value = {"hub", "hubSearch", "hubRoute", "hubRouteSearch", "hubRoutePath"}, allEntries = true)
  public HubResult deleteHub(UserContext userContext, UUID hubId) {
    validateAuth(userContext);

    Hub hub = hubRepository.findById(hubId).orElseThrow(HubNotFoundException::new);

    hub.softDelete(userContext.userId());

      List<HubRoute> relatedRoutes = hubRouteRepository.findAllByOriginHubOrDestinationHub(hub, hub);
      for (HubRoute route : relatedRoutes) {
          route.softDeleteRoute(userContext.userId());
      }

      hubRoutePathQueryService.evictAllPathCache();

    return HubResult.from(hub);
  }

  private void validateAuth(UserContext userContext) {
      if (UserRole.MASTER.equals(userContext.role())) {
          return;
      }

      throw new InvalidAuthException(HubErrorCode.INVALID_AUTH);
  }
}
