package followMe.hub_server.hub.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import followMe.hub_server.hub.application.dto.result.HubResult;
import followMe.hub_server.hub.application.service.HubService;
import followMe.hub_server.hub.application.service.UserRole;
import followMe.hub_server.hub.presentation.dto.request.PostHubReqDto;
import followMe.hub_server.hub.presentation.dto.request.UpdateHubReqDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("HubController 테스트")
class HubControllerTest {

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private HubService hubService;

  @InjectMocks private HubController hubController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(hubController).build();
  }

  @Test
  @DisplayName("허브 생성 요청 성공")
  void createHub_success() throws Exception {
    UUID userId = UUID.randomUUID();

    PostHubReqDto reqDto =
        new PostHubReqDto(
            "서울 허브", "서울특별시 강남구", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

    HubResult result =
        new HubResult(
            UUID.randomUUID(),
            "서울 허브",
            "서울특별시 강남구",
            new BigDecimal("37.4979"),
            new BigDecimal("127.0276"),
            Instant.now(),
            Instant.now(),
            Instant.now());

    given(hubService.createHub(any(), any())).willReturn(result);

    mockMvc
        .perform(
            post("/api/v1/hubs")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", UserRole.MASTER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqDto)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("허브 단건 조회 성공")
  void getHub_success() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID hubId = UUID.randomUUID();

    HubResult result =
        new HubResult(
            hubId,
            "서울 허브",
            "서울특별시 강남구",
            new BigDecimal("37.4979"),
            new BigDecimal("127.0276"),
            Instant.now(),
            Instant.now(),
            Instant.now());

    given(hubService.getHub(eq(hubId))).willReturn(result);

    mockMvc
        .perform(
            get("/api/v1/hubs/{hubId}", hubId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", UserRole.MASTER.name()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("허브 검색 성공")
  void searchHubs_success() throws Exception {
    UUID userId = UUID.randomUUID();

    GetHubsPageResult result = new GetHubsPageResult(List.of(), 0, 10, 300, 100);

    given(hubService.searchHubs(any())).willReturn(result);

    mockMvc
        .perform(
            get("/api/v1/hubs")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", UserRole.MASTER.name())
                .param("keyword", "서울")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("허브 수정 성공")
  void updateHub_success() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID hubId = UUID.randomUUID();

    UpdateHubReqDto reqDto =
        new UpdateHubReqDto(
            "수정된 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

    HubResult result =
        new HubResult(
            hubId,
            "수정된 허브",
            "수정된 주소",
            new BigDecimal("35.1234"),
            new BigDecimal("128.1234"),
            Instant.now(),
            Instant.now(),
            Instant.now());

    given(hubService.updateHub(any(), any())).willReturn(result);

    mockMvc
        .perform(
            patch("/api/v1/hubs/{hubId}", hubId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", UserRole.MASTER.name())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqDto)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("허브 삭제 성공")
  void deleteHub_success() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID hubId = UUID.randomUUID();

    HubResult result =
        new HubResult(
            hubId,
            "서울 허브",
            "서울특별시 강남구",
            new BigDecimal("37.4979"),
            new BigDecimal("127.0276"),
            Instant.now(),
            Instant.now(),
            Instant.now());

    given(hubService.deleteHub(any(), eq(hubId))).willReturn(result);

    mockMvc
        .perform(
            delete("/api/v1/hubs/{hubId}", hubId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", UserRole.MASTER.name()))
        .andExpect(status().isOk());
  }
}
