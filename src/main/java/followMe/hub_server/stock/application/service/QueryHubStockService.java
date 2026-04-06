package followMe.hub_server.stock.application.service;

import followMe.hub_server.stock.application.dto.SearchCondition;
import followMe.hub_server.stock.application.dto.SearchProductIdsRequest;
import followMe.hub_server.stock.application.dto.SearchResponse;
import followMe.hub_server.stock.domain.HubStock;
import followMe.hub_server.stock.domain.HubStockRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QueryHubStockService {
  private final HubStockRepository hubStockRepository;

  // TODO: Query DSL 도입 시 수정
  public Page<SearchResponse> searchHubStock(SearchCondition condition, Pageable pageable) {

    if (Objects.nonNull(condition.getHubId())) {
      return SearchResponse.from(hubStockRepository.findAllByHubId(condition.getHubId(), pageable));
    }
    if (Objects.nonNull(condition.getVendorId())) {
      return SearchResponse.from(
          hubStockRepository.findAllByVendor_Id(condition.getVendorId(), pageable));
    }

    return SearchResponse.from(
        hubStockRepository.findAllByProductId(condition.getProductId(), pageable));
  }

  public List<SearchResponse> searchHubStock(SearchProductIdsRequest request) {

    Set<UUID> ids =
        request.getProducts().stream()
            .map(SearchProductIdsRequest.RequestProduct::getId)
            .collect(Collectors.toSet());
    List<HubStock> stocks = hubStockRepository.findAllByProductIdIn(ids).stream().toList();

    return SearchResponse.listFrom(stocks);
  }
}
