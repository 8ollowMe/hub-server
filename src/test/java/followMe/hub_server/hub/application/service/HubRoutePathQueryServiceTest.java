package followMe.hub_server.hub.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.HubPathResult;
import followMe.hub_server.hub.domain.entity.Hub;
import followMe.hub_server.hub.domain.entity.HubRoute;
import followMe.hub_server.hub.domain.repository.HubRepository;
import followMe.hub_server.hub.domain.repository.HubRouteRepository;
import followMe.hub_server.hub.exception.detail.HubRouteNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("HubRoutePathQueryService 단위 테스트")
class HubRoutePathQueryServiceTest {

  @Mock private HubRepository hubRepository;

  @Mock private HubRouteRepository hubRouteRepository;

  @InjectMocks private HubRoutePathQueryService hubRoutePathQueryService;

  @Test
  @DisplayName("200km 이상 직행 경로는 무시하고 200km 미만 릴레이 경로를 찾는다")
  void getHubPath_withRelayRoute_success() {
    // given
    Hub seoul = hub("11111111-1111-1111-1111-111111111111", "서울특별시 센터");
    Hub daejeon = hub("88888888-8888-8888-8888-888888888888", "대전광역시 센터");
    Hub daegu = hub("55555555-5555-5555-5555-555555555555", "대구광역시 센터");

    HubRoute directOver200 = route(seoul, daegu, "4.0", "250.0");
    HubRoute seoulToDaejeon = route(seoul, daejeon, "2.0", "140.0");
    HubRoute daejeonToDaegu = route(daejeon, daegu, "2.0", "150.0");

    when(hubRepository.findById(seoul.getHubId())).thenReturn(Optional.of(seoul));
    when(hubRepository.findById(daejeon.getHubId())).thenReturn(Optional.of(daejeon));
    when(hubRepository.findById(daegu.getHubId())).thenReturn(Optional.of(daegu));
    when(hubRouteRepository.findAll())
        .thenReturn(List.of(directOver200, seoulToDaejeon, daejeonToDaegu));

    // when
    HubPathResult result = hubRoutePathQueryService.getHubPath(seoul.getHubId(), daegu.getHubId());

    // then
    assertAll(
        () -> assertThat(result.hubNodes()).hasSize(3),
        () -> assertThat(result.hubNodes().get(0).id()).isEqualTo(seoul.getHubId()),
        () -> assertThat(result.hubNodes().get(0).type()).isEqualTo(NodeType.HUB),
        () -> assertThat(result.hubNodes().get(1).id()).isEqualTo(daejeon.getHubId()),
        () -> assertThat(result.hubNodes().get(2).id()).isEqualTo(daegu.getHubId()));
  }

  @Test
  @DisplayName("저장된 단방향 경로를 역방향으로도 사용할 수 있다")
  void getHubPath_reverseDirection_success() {
    // given
    Hub seoul = hub("11111111-1111-1111-1111-111111111111", "서울특별시 센터");
    Hub daejeon = hub("88888888-8888-8888-8888-888888888888", "대전광역시 센터");

    HubRoute seoulToDaejeon = route(seoul, daejeon, "2.0", "140.0");

    when(hubRepository.findById(seoul.getHubId())).thenReturn(Optional.of(seoul));
    when(hubRepository.findById(daejeon.getHubId())).thenReturn(Optional.of(daejeon));
    when(hubRouteRepository.findAll()).thenReturn(List.of(seoulToDaejeon));

    // when
    HubPathResult result =
        hubRoutePathQueryService.getHubPath(daejeon.getHubId(), seoul.getHubId());

    // then
    assertAll(
        () -> assertThat(result.hubNodes()).hasSize(2),
        () -> assertThat(result.hubNodes().get(0).id()).isEqualTo(daejeon.getHubId()),
        () -> assertThat(result.hubNodes().get(1).id()).isEqualTo(seoul.getHubId()));
  }

  @Test
  @DisplayName("200km 미만으로 도달 가능한 경로가 없으면 예외가 발생한다")
  void getHubPath_noAvailableRoute_throwsException() {
    // given
    Hub seoul = hub("11111111-1111-1111-1111-111111111111", "서울특별시 센터");
    Hub busan = hub("44444444-4444-4444-4444-444444444444", "부산광역시 센터");

    HubRoute over200Only = route(seoul, busan, "5.0", "320.0");

    when(hubRepository.findById(seoul.getHubId())).thenReturn(Optional.of(seoul));
    when(hubRepository.findById(busan.getHubId())).thenReturn(Optional.of(busan));
    when(hubRouteRepository.findAll()).thenReturn(List.of(over200Only));

    // when // then
    assertThatThrownBy(
            () -> hubRoutePathQueryService.getHubPath(seoul.getHubId(), busan.getHubId()))
        .isInstanceOf(HubRouteNotFoundException.class);
  }

  private Hub hub(String id, String name) {
    Hub hub =
        Hub.builder()
            .hubName(name)
            .address(name + " 주소")
            .latitude(new BigDecimal("37.0000000"))
            .longitude(new BigDecimal("127.0000000"))
            .build();
    ReflectionTestUtils.setField(hub, "hubId", UUID.fromString(id));
    return hub;
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
