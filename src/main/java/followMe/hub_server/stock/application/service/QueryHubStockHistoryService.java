package followMe.hub_server.stock.application.service;

import followMe.hub_server.stock.application.dto.history.SearchStockHistoryResponse;
import followMe.hub_server.stock.domain.HubStockHistory;
import followMe.hub_server.stock.domain.HubStockHistoryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QueryHubStockHistoryService {
  private final HubStockHistoryRepository historyRepository;

  public Page<SearchStockHistoryResponse> getStockHistory(UUID productId, Pageable pageable) {
    Page<HubStockHistory> histories = historyRepository.findByProductId(productId, pageable);

    return SearchStockHistoryResponse.from(histories);
  }
}
