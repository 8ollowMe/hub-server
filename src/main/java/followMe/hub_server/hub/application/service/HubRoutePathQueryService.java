package followMe.hub_server.hub.application.service;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.HubPathResult;
import followMe.hub_server.hub.application.dto.result.RouteNodeResult;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.domain.repository.HubRouteRepository;
import followMe.hub_server.hub.exception.detail.HubNotFoundException;
import followMe.hub_server.hub.exception.detail.HubRouteNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRoutePathQueryService {

    private static final BigDecimal MAX_SEGMENT_DISTANCE = new BigDecimal("200");

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;

    @Cacheable(
            value = "hubRoutePath",
            key = "#sourceHubId.toString() + ':' + #destinationHubId.toString()")
    public HubPathResult getHubPath(UUID sourceHubId, UUID destinationHubId) {
        Hub sourceHub = hubRepository.findById(sourceHubId).orElseThrow(HubNotFoundException::new);
        Hub destinationHub = hubRepository.findById(destinationHubId).orElseThrow(HubNotFoundException::new);

        List<Hub> hubs = findShortestPath(sourceHub, destinationHub);

        List<RouteNodeResult> hubNodes = new ArrayList<>();
        for (int i = 0; i < hubs.size(); i++) {
            Hub hub = hubs.get(i);
            hubNodes.add(
                    new RouteNodeResult(
                            hub.getHubId(),
                            NodeType.HUB,
                            hub.getHubName(),
                            i + 1));
        }

        return new HubPathResult(hubNodes);
    }

    private List<Hub> findShortestPath(Hub sourceHub, Hub destinationHub) {
        if (sourceHub.getHubId().equals(destinationHub.getHubId())) {
            return List.of(sourceHub);
        }

        // 허브간 이동 정보 모두 가져오기
        List<HubRoute> allRoutes = hubRouteRepository.findAll();

        // 각 허브별로 갈 수 있는 허브 경로 정보 등록
        Map<UUID, List<HubRoute>> graph = new HashMap<>();
        for (HubRoute route : allRoutes) {
            UUID originHubId = route.getOriginHub().getHubId();
            UUID destinationHubId = route.getDestinationHub().getHubId();

            if (route.getDistance().compareTo(MAX_SEGMENT_DISTANCE) < 0) {
                graph.computeIfAbsent(originHubId, key -> new ArrayList<>()).add(route);

                graph.computeIfAbsent(destinationHubId, key -> new ArrayList<>())
                        .add(new HubRoute(
                                route.getDestinationHub(),
                                route.getOriginHub(),
                                route.getDuration(),
                                route.getDistance()
                        ));
            }
        }

        Map<UUID, BigDecimal> distances = new HashMap<>();
        Map<UUID, UUID> previous = new HashMap<>();
        PriorityQueue<RouteState> pq =
                new PriorityQueue<>(Comparator.comparing(RouteState::distance));

        distances.put(sourceHub.getHubId(), BigDecimal.ZERO);
        pq.offer(new RouteState(sourceHub.getHubId(), BigDecimal.ZERO));

        while (!pq.isEmpty()) {
            RouteState current = pq.poll();

            BigDecimal bestDistance = distances.get(current.hubId());
            if (bestDistance != null && current.distance().compareTo(bestDistance) > 0) {
                continue;
            }

            if (current.hubId().equals(destinationHub.getHubId())) {
                break;
            }

            for (HubRoute nextRoute : graph.getOrDefault(current.hubId(), List.of())) {
                UUID nextHubId = nextRoute.getDestinationHub().getHubId();
                BigDecimal nextDistance = current.distance().add(nextRoute.getDistance());

                BigDecimal savedDistance = distances.get(nextHubId);
                if (savedDistance == null || nextDistance.compareTo(savedDistance) < 0) {
                    distances.put(nextHubId, nextDistance);
                    previous.put(nextHubId, current.hubId());
                    pq.offer(new RouteState(nextHubId, nextDistance));
                }
            }
        }

        if (!distances.containsKey(destinationHub.getHubId())) {
            throw new HubRouteNotFoundException();
        }

        LinkedList<Hub> path = new LinkedList<>();
        UUID cursor = destinationHub.getHubId();

        while (cursor != null) {
            Hub hub = hubRepository.findById(cursor).orElseThrow(HubNotFoundException::new);
            path.addFirst(hub);
            cursor = previous.get(cursor);
        }

        return path;
    }

    @CacheEvict(value = "hubRoutePath", allEntries = true)
    public void evictAllPathCache() {}

    private record RouteState(UUID hubId, BigDecimal distance) {}
}
