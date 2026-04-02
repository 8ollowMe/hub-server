package followMe.hub_server.hub.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.followMe.common.pagination.PageRequest;
import followMe.hub_server.hub.application.dto.command.CreateHubCommand;
import followMe.hub_server.hub.application.dto.command.GetHubsQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubCommand;
import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import followMe.hub_server.hub.application.dto.result.HubResult;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.exception.detail.HubNotFoundException;
import followMe.hub_server.hub.exception.detail.InvalidAuthException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("HubServiceImpl 테스트")
class HubServiceImplTest {

  @Mock private HubRepository hubRepository;

  @InjectMocks private HubServiceImpl hubService;

  @Nested
  @DisplayName("허브 생성")
  class CreateHubTest {

    @Test
    @DisplayName("MASTER는 허브를 생성할 수 있다")
    void createHub_master_success() {
      // given
      UUID userId = UUID.randomUUID();
      UserContext userContext = new UserContext(userId, UserRole.MASTER, null, null, null);

      CreateHubCommand command =
          new CreateHubCommand(
              "서울 허브", "서울특별시 강남구", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

      Hub hub =
          Hub.builder()
              .hubName(command.name())
              .address(command.address())
              .latitude(command.latitude())
              .longitude(command.longitude())
              .build();

      when(hubRepository.save(any(Hub.class))).thenReturn(hub);

      // when
      HubResult result = hubService.createHub(userContext, command);

      // then
      assertAll(
          () -> assertThat(result).isNotNull(),
          () -> assertThat(result.name()).isEqualTo("서울 허브"),
          () -> assertThat(result.address()).isEqualTo("서울특별시 강남구"));

      verify(hubRepository).save(any(Hub.class));
    }

    @Test
    @DisplayName("MASTER가 아니면 허브 생성 시 예외가 발생한다")
    void createHub_nonMaster_fail() {
      // given
      UUID userId = UUID.randomUUID();
      UserContext userContext =
          new UserContext(userId, UserRole.HUB_MANAGER, UUID.randomUUID(), null, null);

      CreateHubCommand command =
          new CreateHubCommand(
              "서울 허브", "서울특별시 강남구", new BigDecimal("37.4979"), new BigDecimal("127.0276"));

      // when & then
      assertThatThrownBy(() -> hubService.createHub(userContext, command))
          .isInstanceOf(InvalidAuthException.class);

      verify(hubRepository, never()).save(any(Hub.class));
    }
  }

  @Nested
  @DisplayName("허브 단건 조회")
  class GetHubTest {

    @Test
    @DisplayName("허브가 존재하면 조회에 성공한다")
    void getHub_success() {
      // given
      UUID hubId = UUID.randomUUID();

      Hub hub =
          Hub.builder()
              .hubName("서울 허브")
              .address("서울특별시 강남구")
              .latitude(new BigDecimal("37.4979"))
              .longitude(new BigDecimal("127.0276"))
              .build();

      when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));

      // when
      HubResult result = hubService.getHub(hubId);

      // then
      assertAll(
          () -> assertThat(result).isNotNull(), () -> assertThat(result.name()).isEqualTo("서울 허브"));
    }

    @Test
    @DisplayName("허브가 없으면 예외가 발생한다")
    void getHub_notFound() {
      // given
      UUID hubId = UUID.randomUUID();

      when(hubRepository.findById(hubId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> hubService.getHub(hubId)).isInstanceOf(HubNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("허브 검색")
  class SearchHubsTest {

    @Test
    @DisplayName("키워드로 허브 목록을 조회할 수 있다")
    void searchHubs_success() {
      // given
      GetHubsQuery query = new GetHubsQuery("서울", PageRequest.of(0, 10));

      Hub hub =
          Hub.builder()
              .hubName("서울 허브")
              .address("서울특별시 강남구")
              .latitude(new BigDecimal("37.4979"))
              .longitude(new BigDecimal("127.0276"))
              .build();

      Page<Hub> page =
          new PageImpl<>(List.of(hub), org.springframework.data.domain.PageRequest.of(0, 10), 1);

      when(hubRepository.searchByKeyword(
              anyString(), any(org.springframework.data.domain.Pageable.class)))
          .thenReturn(page);

      // when
      GetHubsPageResult result = hubService.searchHubs(query);

      // then
      assertAll(
          () -> assertThat(result).isNotNull(), () -> assertThat(result.content()).hasSize(1));
    }
  }

  @Nested
  @DisplayName("허브 수정")
  class UpdateHubTest {

    @Test
    @DisplayName("MASTER는 모든 허브를 수정할 수 있다")
    void updateHub_master_success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID hubId = UUID.randomUUID();

      UserContext userContext = new UserContext(userId, UserRole.MASTER, null, null, null);
      UpdateHubCommand command =
          new UpdateHubCommand(
              hubId, "수정된 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      Hub hub = mock(Hub.class);

      when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
      when(hub.getHubName()).thenReturn("수정된 허브");
      when(hub.getAddress()).thenReturn("수정된 주소");
      when(hub.getLatitude()).thenReturn(new BigDecimal("35.1234"));
      when(hub.getLongitude()).thenReturn(new BigDecimal("128.1234"));
      when(hub.getCreatedAt()).thenReturn(Instant.now());
      when(hub.getUpdatedAt()).thenReturn(Instant.now());

      // when
      HubResult result = hubService.updateHub(userContext, command);

      // then
      assertThat(result).isNotNull();
      verify(hub).update("수정된 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));
    }

    @Test
    @DisplayName("자신의 허브와 일치하는 HUB_MANAGER는 수정할 수 있다")
    void updateHub_hubManager_success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID hubId = UUID.randomUUID();

      UserContext userContext = new UserContext(userId, UserRole.HUB_MANAGER, hubId, null, null);
      UpdateHubCommand command =
          new UpdateHubCommand(
              hubId, "수정된 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      Hub hub = mock(Hub.class);

      when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
      when(hub.getHubName()).thenReturn("수정된 허브");
      when(hub.getAddress()).thenReturn("수정된 주소");
      when(hub.getLatitude()).thenReturn(new BigDecimal("35.1234"));
      when(hub.getLongitude()).thenReturn(new BigDecimal("128.1234"));
      when(hub.getCreatedAt()).thenReturn(Instant.now());
      when(hub.getUpdatedAt()).thenReturn(Instant.now());

      // when
      HubResult result = hubService.updateHub(userContext, command);

      // then
      assertThat(result).isNotNull();
      verify(hub).update("수정된 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));
    }

    @Test
    @DisplayName("다른 허브의 HUB_MANAGER는 수정할 수 없다")
    void updateHub_otherHubManager_fail() {
      // given
      UUID userId = UUID.randomUUID();
      UUID myHubId = UUID.randomUUID();
      UUID targetHubId = UUID.randomUUID();

      UserContext userContext = new UserContext(userId, UserRole.HUB_MANAGER, myHubId, null, null);
      UpdateHubCommand command =
          new UpdateHubCommand(
              targetHubId,
              "수정된 허브",
              "수정된 주소",
              new BigDecimal("35.1234"),
              new BigDecimal("128.1234"));

      // when & then
      assertThatThrownBy(() -> hubService.updateHub(userContext, command))
          .isInstanceOf(InvalidAuthException.class);

      verify(hubRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("권한 없는 사용자는 허브를 수정할 수 없다")
    void updateHub_invalidRole_fail() {
      // given
      UUID userId = UUID.randomUUID();
      UUID hubId = UUID.randomUUID();

      UserContext userContext =
          new UserContext(userId, UserRole.DELIVERY_MANAGER, null, null, null);
      UpdateHubCommand command =
          new UpdateHubCommand(
              hubId, "수정된 허브", "수정된 주소", new BigDecimal("35.1234"), new BigDecimal("128.1234"));

      // when & then
      assertThatThrownBy(() -> hubService.updateHub(userContext, command))
          .isInstanceOf(InvalidAuthException.class);

      verify(hubRepository, never()).findById(any(UUID.class));
    }
  }

  @Nested
  @DisplayName("허브 삭제")
  class DeleteHubTest {

    @Test
    @DisplayName("MASTER는 허브를 삭제할 수 있다")
    void deleteHub_master_success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID hubId = UUID.randomUUID();

      UserContext userContext = new UserContext(userId, UserRole.MASTER, null, null, null);
      Hub hub = mock(Hub.class);

      when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
      when(hub.getCreatedAt()).thenReturn(Instant.now());
      when(hub.getUpdatedAt()).thenReturn(Instant.now());

      // when
      HubResult result = hubService.deleteHub(userContext, hubId);

      // then
      assertThat(result).isNotNull();
      verify(hub).softDelete(userId);
    }

    @Test
    @DisplayName("자신의 허브와 일치하는 HUB_MANAGER는 삭제할 수 있다")
    void deleteHub_hubManager_success() {
      // given
      UUID userId = UUID.randomUUID();
      UUID hubId = UUID.randomUUID();

      UserContext userContext = new UserContext(userId, UserRole.HUB_MANAGER, hubId, null, null);
      Hub hub = mock(Hub.class);

      when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
      when(hub.getCreatedAt()).thenReturn(Instant.now());
      when(hub.getUpdatedAt()).thenReturn(Instant.now());

      // when
      HubResult result = hubService.deleteHub(userContext, hubId);

      // then
      assertThat(result).isNotNull();
      verify(hub).softDelete(userId);
    }

    @Test
    @DisplayName("다른 허브의 HUB_MANAGER는 삭제할 수 없다")
    void deleteHub_otherHubManager_fail() {
      // given
      UUID userId = UUID.randomUUID();
      UUID myHubId = UUID.randomUUID();
      UUID targetHubId = UUID.randomUUID();

      UserContext userContext = new UserContext(userId, UserRole.HUB_MANAGER, myHubId, null, null);

      // when & then
      assertThatThrownBy(() -> hubService.deleteHub(userContext, targetHubId))
          .isInstanceOf(InvalidAuthException.class);

      verify(hubRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("권한 없는 사용자는 허브를 삭제할 수 없다")
    void deleteHub_invalidRole_fail() {
      // given
      UUID userId = UUID.randomUUID();
      UUID hubId = UUID.randomUUID();

      UserContext userContext =
          new UserContext(userId, UserRole.DELIVERY_MANAGER, null, null, null);

      // when & then
      assertThatThrownBy(() -> hubService.deleteHub(userContext, hubId))
          .isInstanceOf(InvalidAuthException.class);

      verify(hubRepository, never()).findById(any(UUID.class));
    }
  }
}
