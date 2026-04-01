package followMe.hub_server.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.presentation.dto.request.PostHubReqDto;
import followMe.hub_server.hub.presentation.dto.request.UpdateHubReqDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(HubIntegrationTest.JpaTestScanConfig.class)
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
@DisplayName("Hub 통합 테스트")
class HubIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private HubRepository hubRepository;

  @TestConfiguration
  @EntityScan(basePackages = {"followMe.hub_server.hub.domain.entity"})
  @EnableJpaRepositories(basePackages = {"followMe.hub_server.hub.domain.repository"})
  static class JpaTestScanConfig {}

  @Nested
  @DisplayName("허브 생성")
  class CreateHubTest {

    @Test
    @DisplayName("MASTER는 허브를 생성할 수 있다")
    void createHub_master_success() throws Exception {
      UUID userId = UUID.randomUUID();

      PostHubReqDto reqDto =
          new PostHubReqDto(
              "서울 허브", "서울특별시 강남구", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

      mockMvc
          .perform(
              post("/api/v1/hubs")
                  .with(csrf())
                  .header("X-User-Id", userId.toString())
                  .header("X-User-Role", "MASTER")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isCreated());

      Hub savedHub = findHubByKeyword("서울 허브");
      assertThat(savedHub).isNotNull();
      assertThat(savedHub.getHubName()).isEqualTo("서울 허브");
    }

    @Test
    @DisplayName("HUB_MANAGER는 허브를 생성할 수 없다")
    void createHub_hubManager_fail() throws Exception {
      UUID userId = UUID.randomUUID();

      PostHubReqDto reqDto =
          new PostHubReqDto(
              "서울 허브", "서울특별시 강남구", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

      mockMvc
          .perform(
              post("/api/v1/hubs")
                  .header("X-User-Id", userId.toString())
                  .header("X-User-Role", "HUB_MANAGER")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("권한 없는 사용자는 허브를 생성할 수 없다")
    void createHub_invalidRole_fail() throws Exception {
      UUID userId = UUID.randomUUID();

      PostHubReqDto reqDto =
          new PostHubReqDto(
              "서울 허브", "서울특별시 강남구", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

      mockMvc
          .perform(
              post("/api/v1/hubs")
                  .header("X-User-Id", userId.toString())
                  .header("X-User-Role", "DELIVERY_MANAGER")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("허브 조회")
  class ReadHubTest {

    @Test
    @DisplayName("허브 단건 조회에 성공한다")
    void getHub_success() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      mockMvc
          .perform(
              get("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", UUID.randomUUID().toString())
                  .header("X-User-Role", "MASTER"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("허브 검색에 성공한다")
    void searchHubs_success() throws Exception {
      UUID masterId = UUID.randomUUID();

      createHubAndGetId(masterId, "서울 허브");
      createHubAndGetId(masterId, "부산 허브");

      mockMvc
          .perform(
              get("/api/v1/hubs")
                  .header("X-User-Id", UUID.randomUUID().toString())
                  .header("X-User-Role", "MASTER")
                  .param("keyword", "서울")
                  .param("page", "0")
                  .param("size", "10"))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("허브 수정")
  class UpdateHubTest {

    @Test
    @DisplayName("MASTER는 모든 허브를 수정할 수 있다")
    void updateHub_master_success() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      UpdateHubReqDto reqDto =
          new UpdateHubReqDto(
              "수정된 서울 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      mockMvc
          .perform(
              patch("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", masterId.toString())
                  .header("X-User-Role", "MASTER")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isOk());

      Hub updatedHub = hubRepository.findById(hubId).orElseThrow();
      assertThat(updatedHub.getHubName()).isEqualTo("수정된 서울 허브");
      assertThat(updatedHub.getAddress()).isEqualTo("수정된 주소");
    }

    @Test
    @DisplayName("자신의 허브인 HUB_MANAGER는 수정할 수 있다")
    void updateHub_myHubManager_success() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID managerId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      UpdateHubReqDto reqDto =
          new UpdateHubReqDto(
              "수정된 서울 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      mockMvc
          .perform(
              patch("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", managerId.toString())
                  .header("X-User-Role", "HUB_MANAGER")
                  .header("X-Hub-Id", hubId.toString())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isOk());

      Hub updatedHub = hubRepository.findById(hubId).orElseThrow();
      assertThat(updatedHub.getHubName()).isEqualTo("수정된 서울 허브");
    }

    @Test
    @DisplayName("다른 허브의 HUB_MANAGER는 수정할 수 없다")
    void updateHub_otherHubManager_fail() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID managerId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      UpdateHubReqDto reqDto =
          new UpdateHubReqDto(
              "수정된 서울 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      mockMvc
          .perform(
              patch("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", managerId.toString())
                  .header("X-User-Role", "HUB_MANAGER")
                  .header("X-Hub-Id", UUID.randomUUID().toString())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("권한 없는 사용자는 허브를 수정할 수 없다")
    void updateHub_invalidRole_fail() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      UpdateHubReqDto reqDto =
          new UpdateHubReqDto(
              "수정된 서울 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      mockMvc
          .perform(
              patch("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", UUID.randomUUID().toString())
                  .header("X-User-Role", "DELIVERY_MANAGER")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(reqDto)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("허브 삭제")
  class DeleteHubTest {

    @Test
    @DisplayName("MASTER는 허브를 삭제할 수 있다")
    void deleteHub_master_success() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      mockMvc
          .perform(
              delete("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", masterId.toString())
                  .header("X-User-Role", "MASTER"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("자신의 허브인 HUB_MANAGER는 삭제할 수 있다")
    void deleteHub_myHubManager_success() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID managerId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      mockMvc
          .perform(
              delete("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", managerId.toString())
                  .header("X-User-Role", "HUB_MANAGER")
                  .header("X-Hub-Id", hubId.toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("다른 허브의 HUB_MANAGER는 삭제할 수 없다")
    void deleteHub_otherHubManager_fail() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID managerId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      mockMvc
          .perform(
              delete("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", managerId.toString())
                  .header("X-User-Role", "HUB_MANAGER")
                  .header("X-Hub-Id", UUID.randomUUID().toString()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("권한 없는 사용자는 허브를 삭제할 수 없다")
    void deleteHub_invalidRole_fail() throws Exception {
      UUID masterId = UUID.randomUUID();
      UUID hubId = createHubAndGetId(masterId, "서울 허브");

      mockMvc
          .perform(
              delete("/api/v1/hubs/{hubId}", hubId)
                  .header("X-User-Id", UUID.randomUUID().toString())
                  .header("X-User-Role", "DELIVERY_MANAGER"))
          .andExpect(status().isUnauthorized());
    }
  }

  private UUID createHubAndGetId(UUID userId, String name) throws Exception {
    PostHubReqDto reqDto =
        new PostHubReqDto(
            name, name + " 주소", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/hubs")
                    .header("X-User-Id", userId.toString())
                    .header("X-User-Role", "MASTER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reqDto)))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    UUID hubIdFromResponse = extractHubId(responseBody);

    if (hubIdFromResponse != null) {
      return hubIdFromResponse;
    }

    return findHubByKeyword(name).getHubId();
  }

  private UUID extractHubId(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);

    JsonNode idNode = root.path("data").path("hubId");
    if (!idNode.isMissingNode() && !idNode.isNull()) {
      return UUID.fromString(idNode.asText());
    }

    idNode = root.path("data").path("id");
    if (!idNode.isMissingNode() && !idNode.isNull()) {
      return UUID.fromString(idNode.asText());
    }

    return null;
  }

  private Hub findHubByKeyword(String keyword) {
    return hubRepository.searchByKeyword(keyword, PageRequest.of(0, 1)).getContent().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("생성된 허브를 찾을 수 없습니다. keyword=" + keyword));
  }
}
