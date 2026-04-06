package followMe.hub_server.hub;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.domain.repository.HubRouteRepository;
import followMe.hub_server.hub.infrastructure.client.VendorClient;
import followMe.hub_server.hub.infrastructure.client.dto.VendorResponse;
import followMe.hub_server.hub.infrastructure.client.enums.VendorType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.cloud.config.enabled=false",
      "spring.config.import=",
      "eureka.client.enabled=false",
      "spring.cloud.discovery.enabled=false",
      "spring.cache.type=simple"
    })
@DisplayName("HubRoute 통합 테스트")
class HubRouteIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private HubRepository hubRepository;

  @Autowired private CacheManager cacheManager;

  @Autowired private HubRouteRepository realHubRouteRepository;

  @MockitoSpyBean private HubRouteRepository hubRouteRepository;

  @MockitoBean private VendorClient vendorClient;

  @MockitoBean(name = "auditorAware")
  private AuditorAware<UUID> auditorAware;

  private UUID seoulHubId;
  private UUID daejeonHubId;
  private UUID daeguHubId;
  private UUID vendorId;

  @BeforeEach
  void setUp() {
    when(auditorAware.getCurrentAuditor())
        .thenReturn(Optional.of(UUID.fromString("11111111-1111-1111-1111-111111111111")));

    realHubRouteRepository.deleteAll();
    hubRepository.deleteAll();

    if (cacheManager.getCache("hubRoutePath") != null) {
      cacheManager.getCache("hubRoutePath").clear();
    }

    Hub seoul = hub("서울특별시 센터", "서울특별시 송파구 송파대로 55");
    Hub daejeon = hub("대전광역시 센터", "대전 서구 둔산로 100");
    Hub daegu = hub("대구광역시 센터", "대구 북구 태평로 161");

    seoul = hubRepository.save(seoul);
    daejeon = hubRepository.save(daejeon);
    daegu = hubRepository.save(daegu);

    realHubRouteRepository.save(route(seoul, daejeon, "2.0", "140.0"));
    realHubRouteRepository.save(route(daejeon, daegu, "2.0", "150.0"));
    realHubRouteRepository.save(route(seoul, daegu, "4.0", "250.0"));

    seoulHubId = seoul.getHubId();
    daejeonHubId = daejeon.getHubId();
    daeguHubId = daegu.getHubId();
    vendorId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");

    when(vendorClient.getVendor(vendorId))
        .thenReturn(
            new VendorResponse(
                vendorId,
                "대구업체",
                VendorType.BUYER,
                "설명",
                daeguHubId,
                "사장님",
                "대구 주소",
                35.87,
                128.60));

    clearInvocations(hubRouteRepository);
  }

  @Test
  @DisplayName("경로 조회 API는 HUB 노드들과 마지막 VENDOR 노드를 반환한다")
  void getRoute_success() throws Exception {
    mockMvc
        .perform(
            get("/api/hubs/route")
                .param("sourceHubId", seoulHubId.toString())
                .param("vendorId", vendorId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(4))
        .andExpect(jsonPath("$.nodes[0].id").value(seoulHubId.toString()))
        .andExpect(jsonPath("$.nodes[0].type").value("HUB"))
        .andExpect(jsonPath("$.nodes[1].id").value(daejeonHubId.toString()))
        .andExpect(jsonPath("$.nodes[1].type").value("HUB"))
        .andExpect(jsonPath("$.nodes[2].id").value(daeguHubId.toString()))
        .andExpect(jsonPath("$.nodes[2].type").value("HUB"))
        .andExpect(jsonPath("$.nodes[3].id").value(vendorId.toString()))
        .andExpect(jsonPath("$.nodes[3].type").value("VENDOR"))
        .andExpect(jsonPath("$.nodes[3].name").value("대구업체"))
        .andExpect(jsonPath("$.nodes[3].sequence").value(4));
  }

  @Test
  @DisplayName("같은 출발지와 목적지로 두 번 조회하면 hub path 캐시가 적용된다")
  void getRoute_cacheApplied() throws Exception {
    mockMvc
        .perform(
            get("/api/hubs/route")
                .param("sourceHubId", seoulHubId.toString())
                .param("vendorId", vendorId.toString()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/hubs/route")
                .param("sourceHubId", seoulHubId.toString())
                .param("vendorId", vendorId.toString()))
        .andExpect(status().isOk());

    verify(hubRouteRepository, times(1)).findAll();
  }

  private Hub hub(String name, String address) {
    return Hub.builder()
        .hubName(name)
        .address(address)
        .latitude(new BigDecimal("37.0000000"))
        .longitude(new BigDecimal("127.0000000"))
        .build();
  }

  private HubRoute route(Hub origin, Hub destination, String duration, String distance) {
    return HubRoute.builder()
        .originHub(origin)
        .destinationHub(destination)
        .duration(new BigDecimal(duration))
        .distance(new BigDecimal(distance))
        .build();
  }
}
