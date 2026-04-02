package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.HubPathResult;
import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import followMe.hub_server.hub.application.dto.result.RouteNodeResult;
import followMe.hub_server.hub.exception.detail.InvalidVendorResponseException;
import followMe.hub_server.hub.exception.detail.VendorNotFoundException;
import followMe.hub_server.hub.infrastructure.client.VendorClient;
import followMe.hub_server.hub.infrastructure.client.dto.VendorResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteServiceImpl implements HubRouteService {

  private final VendorClient vendorClient;
  private final HubRoutePathQueryService hubRoutePathQueryService;

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
            vendorResponse.vendorId(), NodeType.VENDOR, vendorResponse.name(), nodes.size() + 1));

    return new HubRouteResult(nodes);
  }
}
