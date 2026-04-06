package followMe.hub_server.hub.presentation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import followMe.hub_server.common.config.JpaAuditConfig;
import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import followMe.hub_server.hub.application.dto.result.RouteNodeResult;
import followMe.hub_server.hub.application.service.HubRouteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = HubRouteController.class,
    excludeFilters = {
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditConfig.class)
    })
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(
    properties = {
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.cloud.discovery.enabled=false",
      "spring.config.import=",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
    })
@DisplayName("HubRouteController 단위 테스트")
class HubRouteControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HubRouteService hubRouteService;

  @MockitoBean private JpaMetamodelMappingContext jpaMappingContext;

  @Test
  @DisplayName("경로 조회 요청 시 nodes를 반환한다")
  void getRoute_success() throws Exception {
    UUID sourceHubId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID vendorId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");

    HubRouteResult result =
        new HubRouteResult(
            List.of(
                new RouteNodeResult(sourceHubId, NodeType.HUB, "서울특별시 센터", 1),
                new RouteNodeResult(
                    UUID.fromString("88888888-8888-8888-8888-888888888888"),
                    NodeType.HUB,
                    "대전광역시 센터",
                    2),
                new RouteNodeResult(
                    UUID.fromString("55555555-5555-5555-5555-555555555555"),
                    NodeType.HUB,
                    "대구광역시 센터",
                    3),
                new RouteNodeResult(vendorId, NodeType.VENDOR, "대구업체", 4)));

    when(hubRouteService.getRoute(sourceHubId, vendorId)).thenReturn(result);

    mockMvc
        .perform(
            get("/api/hubs/route")
                .param("sourceHubId", sourceHubId.toString())
                .param("vendorId", vendorId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes[0].type").value("HUB"))
        .andExpect(jsonPath("$.nodes[0].name").value("서울특별시 센터"))
        .andExpect(jsonPath("$.nodes[3].type").value("VENDOR"))
        .andExpect(jsonPath("$.nodes[3].name").value("대구업체"))
        .andExpect(jsonPath("$.nodes[3].sequence").value(4));
  }
}
