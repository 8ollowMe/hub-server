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

  // 한 번에 직접 이동 가능한 최대 거리 기준
  // 이 거리보다 짧은 경로만 그래프에 포함시켜서 경로 탐색에 사용
  private static final BigDecimal MAX_SEGMENT_DISTANCE = new BigDecimal("200");

  private final HubRepository hubRepository;
  private final HubRouteRepository hubRouteRepository;

  @Cacheable(
      value = "hubRoutePath",
      key = "#sourceHubId.toString() + ':' + #destinationHubId.toString()")
  public HubPathResult getHubPath(UUID sourceHubId, UUID destinationHubId) {
    Hub sourceHub = hubRepository.findById(sourceHubId).orElseThrow(HubNotFoundException::new);
    Hub destinationHub =
        hubRepository.findById(destinationHubId).orElseThrow(HubNotFoundException::new);

    List<Hub> hubs = findShortestPath(sourceHub, destinationHub);

    List<RouteNodeResult> hubNodes = new ArrayList<>();
    for (int i = 0; i < hubs.size(); i++) {
      Hub hub = hubs.get(i);
        if (i < hubs.size() - 1) {
            Hub nextHub = hubs.get(i + 1);
            HubRoute route = hubRouteRepository.findByOriginHubAndDestinationHub(hub, nextHub).orElseThrow(HubRouteNotFoundException::new);
            hubNodes.add(new RouteNodeResult(hub.getHubId(), NodeType.HUB, hub.getHubName(), route.getDuration(), route.getDistance(), i + 1));
        } else {
            hubNodes.add(new RouteNodeResult(hub.getHubId(), NodeType.HUB, hub.getHubName(), null, null, i + 1));
        }
    }

    return new HubPathResult(hubNodes);
  }

  private List<Hub> findShortestPath(Hub sourceHub, Hub destinationHub) {
    // 출발 허브와 도착 허브가 같으면 이동이 필요 없으므로 자기 자신만 반환
    if (sourceHub.getHubId().equals(destinationHub.getHubId())) {
      return List.of(sourceHub);
    }

    // DB에 저장된 허브 간 이동 정보 전체 조회
    // 이후 이 데이터를 기반으로 그래프를 구성
    List<HubRoute> allRoutes = hubRouteRepository.findAll();

    // 그래프 구조
    // key: 출발 허브 ID
    // value: 해당 허브에서 이동 가능한 HubRoute 목록
    Map<UUID, List<HubRoute>> graph = new HashMap<>();

    // 전체 경로 정보를 순회하면서 그래프를 구성
    for (HubRoute route : allRoutes) {
      UUID originHubId = route.getOriginHub().getHubId();
      UUID destinationHubId = route.getDestinationHub().getHubId();

      // 직접 이동 가능한 최대 거리보다 짧은 경로만 그래프에 포함
      if (route.getDistance().compareTo(MAX_SEGMENT_DISTANCE) < 0) {
        // 정방향 경로 추가
        // originHubId -> destinationHubId
        graph.computeIfAbsent(originHubId, key -> new ArrayList<>()).add(route);

        // 역방향 경로도 추가
        // destinationHubId -> originHubId 형태의 HubRoute를 새로 만들어 넣음
        graph
            .computeIfAbsent(destinationHubId, key -> new ArrayList<>())
            .add(
                new HubRoute(
                    route.getDestinationHub(),
                    route.getOriginHub(),
                    route.getDuration(),
                    route.getDistance()));
      }
    }

    /*---다익스트라 알고리즘용 자료구조---*/

    // 출발지로부터 각 허브까지의 현재까지 알려진 최단 거리 저장
    Map<UUID, BigDecimal> distances = new HashMap<>();

    // 최단 경로를 복원하기 위해 특정 허브에 오기 직전의 허브 ID를 저장
    Map<UUID, UUID> previous = new HashMap<>();

    // 가장 짧은 거리 상태를 우선 꺼내기 위한 우선순위 큐
    // distance 기준 오름차순 정렬
    PriorityQueue<RouteState> pq = new PriorityQueue<>(Comparator.comparing(RouteState::distance));

    // 시작 허브까지의 거리는 0
    distances.put(sourceHub.getHubId(), BigDecimal.ZERO);

    // 우선순위 큐에 시작 상태 삽입
    pq.offer(new RouteState(sourceHub.getHubId(), BigDecimal.ZERO));

    /*---다익스트라 알고리즘 수행---*/
    while (!pq.isEmpty()) {
      // 현재까지 가장 짧은 거리 후보를 꺼냄
      RouteState current = pq.poll();

      // 현재 허브에 대해 이미 더 짧은 경로가 저장되어 있다면 지금 꺼낸 정보는 오래된 정보이므로 무시
      BigDecimal bestDistance = distances.get(current.hubId());
      if (bestDistance != null && current.distance().compareTo(bestDistance) > 0) {
        continue;
      }

      // 도착 허브에 도달했다면 종료 (우선순위 큐 특성상 이 시점의 거리가 최단 거리)
      if (current.hubId().equals(destinationHub.getHubId())) {
        break;
      }

      // 현재 허브에서 갈 수 있는 다음 허브들을 확인
      for (HubRoute nextRoute : graph.getOrDefault(current.hubId(), List.of())) {
        UUID nextHubId = nextRoute.getDestinationHub().getHubId();

        // 현재까지 거리 + 다음 간선 거리
        BigDecimal nextDistance = current.distance().add(nextRoute.getDistance());

        // 기존에 저장된 다음 허브까지의 거리 조회
        BigDecimal savedDistance = distances.get(nextHubId);

        // 아직 방문하지 않았거나 이번에 계산한 거리가 더 짧으면 최단 거리 갱신
        if (savedDistance == null || nextDistance.compareTo(savedDistance) < 0) {
          distances.put(nextHubId, nextDistance);

          // nextHubId에 도달하기 직전 허브를 기록
          previous.put(nextHubId, current.hubId());

          // 갱신된 거리 정보로 우선순위 큐에 추가
          pq.offer(new RouteState(nextHubId, nextDistance));
        }
      }
    }

    // 도착 허브까지의 최단 거리가 끝내 계산되지 않았다면 갈 수 있는 경로가 없다는 의미
    if (!distances.containsKey(destinationHub.getHubId())) {
      throw new HubRouteNotFoundException();
    }

    // previous 맵을 이용해 도착지부터 출발지까지 역추적하여 실제 경로 복원
    LinkedList<Hub> path = new LinkedList<>();
    UUID cursor = destinationHub.getHubId();

    while (cursor != null) {
      // 허브 ID로 실제 Hub 엔티티 조회
      Hub hub = hubRepository.findById(cursor).orElseThrow(HubNotFoundException::new);

      // 역추적 중이므로 앞에 삽입해서 출발지 -> 도착지 순서로 맞춤
      path.addFirst(hub);

      // 직전 허브로 이동
      cursor = previous.get(cursor);
    }

    return path;
  }

  @CacheEvict(value = "hubRoutePath", allEntries = true)
  public void evictAllPathCache() {}

  private record RouteState(UUID hubId, BigDecimal distance) {}
}
