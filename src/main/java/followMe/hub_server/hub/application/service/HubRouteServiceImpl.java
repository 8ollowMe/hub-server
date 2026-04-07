package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.command.CreateHubRouteCommand;
import followMe.hub_server.hub.application.dto.command.SearchHubRoutesQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubRouteCommand;
import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.*;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.domain.repository.HubRouteRepository;
import followMe.hub_server.hub.exception.detail.*;
import followMe.hub_server.hub.infrastructure.client.VendorClient;
import followMe.hub_server.hub.infrastructure.client.dto.VendorResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteServiceImpl implements HubRouteService {

  private final HubRepository hubRepository;
  private final HubRouteRepository hubRouteRepository;
  private final VendorClient vendorClient;
  private final HubRoutePathQueryService hubRoutePathQueryService;

  @Override
  @Transactional
  @CacheEvict(
      value = {"hubRoute", "hubRouteSearch", "hubRoutePath"},
      allEntries = true)
  public HubRouteDetailResult create(UserContext userContext, CreateHubRouteCommand command) {
    validateAuth(userContext);

    Hub originHub =
        hubRepository.findById(command.originHubId()).orElseThrow(HubNotFoundException::new);
    Hub destinationHub =
        hubRepository.findById(command.destinationHubId()).orElseThrow(HubNotFoundException::new);

    if (hubRouteRepository.existsByOriginHubAndDestinationHub(originHub, destinationHub)) {
      throw new HubRouteAlreadyExistsException();
    }

    HubRoute hubRoute =
        HubRoute.builder()
            .originHub(originHub)
            .destinationHub(destinationHub)
            .duration(command.duration())
            .distance(command.distance())
            .build();

    return HubRouteDetailResult.from(hubRouteRepository.save(hubRoute));
  }

  @Override
  @Cacheable(value = "hubRoute", key = "#hubRouteId")
  public HubRouteDetailResult get(UUID hubRouteId) {
    return HubRouteDetailResult.from(
        hubRouteRepository
            .findByHubRouteId(hubRouteId)
            .orElseThrow(HubRouteNotFoundException::new));
  }

  @Override
  @Cacheable(
      value = "hubRouteSearch",
      key =
          "#query.originHubId() + ':' + #query.destinationHubId() + ':' + #query.pageRequest().page + ':' + #query.pageRequest().size")
  public HubRoutePageResult search(SearchHubRoutesQuery query) {
    return HubRoutePageResult.from(
        hubRouteRepository.search(
            query.originHubId(), query.destinationHubId(), query.pageRequest().toPageable()));
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {"hubRoute", "hubRouteSearch", "hubRoutePath"},
      allEntries = true)
  public HubRouteDetailResult update(UserContext userContext, UpdateHubRouteCommand command) {
    validateAuth(userContext);

    HubRoute hubRoute =
        hubRouteRepository
            .findByHubRouteId(command.hubRouteId())
            .orElseThrow(HubRouteNotFoundException::new);

    Hub originHub =
        hubRepository.findById(command.originHubId()).orElseThrow(HubNotFoundException::new);
    Hub destinationHub =
        hubRepository.findById(command.destinationHubId()).orElseThrow(HubNotFoundException::new);

    hubRoute.update(originHub, destinationHub, command.duration(), command.distance());
    return HubRouteDetailResult.from(hubRoute);
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {"hubRoute", "hubRouteSearch", "hubRoutePath"},
      allEntries = true)
  public HubRouteDetailResult delete(UserContext userContext, UUID hubRouteId) {
    validateAuth(userContext);

    HubRoute hubRoute =
        hubRouteRepository.findByHubRouteId(hubRouteId).orElseThrow(HubRouteNotFoundException::new);
    hubRoute.softDeleteRoute(userContext.userId());
    return HubRouteDetailResult.from(hubRoute);
  }

  @Override
  public HubRouteResult getRoute(UUID sourceHubId, UUID vendorId) {
    VendorResponse vendorResponse = vendorClient.getVendor(vendorId);

    if (vendorResponse == null) {
      throw new VendorNotFoundException();
    }
    if (vendorResponse.hubId() == null) {
      throw new InvalidVendorResponseException();
    }

    // 허브 간 경로 계산
    HubPathResult hubPathResult =
        hubRoutePathQueryService.getHubPath(sourceHubId, vendorResponse.hubId());

    List<RouteNodeResult> nodes = new ArrayList<>(hubPathResult.hubNodes());

    nodes.add(
        new RouteNodeResult(
            vendorResponse.vendorId(),
            NodeType.VENDOR,
            vendorResponse.name(),
            null,
            null,
            nodes.size() + 1));

    return new HubRouteResult(nodes);
  }

  private void validateAuth(UserContext userContext) {}
}
