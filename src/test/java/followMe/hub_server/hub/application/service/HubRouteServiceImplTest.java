package followMe.hub_server.hub.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import followMe.hub_server.hub.application.dto.enums.NodeType;
import followMe.hub_server.hub.application.dto.result.HubPathResult;
import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import followMe.hub_server.hub.application.dto.result.RouteNodeResult;
import followMe.hub_server.hub.exception.detail.InvalidVendorResponseException;
import followMe.hub_server.hub.exception.detail.VendorNotFoundException;
import followMe.hub_server.hub.infrastructure.client.VendorClient;
import followMe.hub_server.hub.infrastructure.client.dto.VendorResponse;
import followMe.hub_server.hub.infrastructure.client.enums.VendorType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HubRouteServiceImpl 단위 테스트")
class HubRouteServiceImplTest {

    @Mock
    private VendorClient vendorClient;

    @Mock
    private HubRoutePathQueryService hubRoutePathQueryService;

    @InjectMocks
    private HubRouteServiceImpl hubRouteService;

    @Test
    @DisplayName("vendor 정보와 hub path를 합쳐 최종 route를 반환한다")
    void getRoute_success() {
        // given
        UUID sourceHubId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID middleHubId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID destinationHubId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID vendorId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");

        VendorResponse vendorResponse = new VendorResponse(
                vendorId,
                "대구업체",
                VendorType.BUYER,
                "설명",
                destinationHubId,
                "사장님",
                "대구 주소",
                35.87,
                128.60
        );

        HubPathResult hubPathResult = new HubPathResult(List.of(
                new RouteNodeResult(sourceHubId, NodeType.HUB, "서울특별시 센터", 1),
                new RouteNodeResult(middleHubId, NodeType.HUB, "대전광역시 센터", 2),
                new RouteNodeResult(destinationHubId, NodeType.HUB, "대구광역시 센터", 3)
        ));

        when(vendorClient.getVendor(vendorId)).thenReturn(vendorResponse);
        when(hubRoutePathQueryService.getHubPath(sourceHubId, destinationHubId)).thenReturn(hubPathResult);

        // when
        HubRouteResult result = hubRouteService.getRoute(sourceHubId, vendorId);

        // then
        assertAll(
                () -> assertThat(result.nodes()).hasSize(4),
                () -> assertThat(result.nodes().get(0).type()).isEqualTo(NodeType.HUB),
                () -> assertThat(result.nodes().get(3).id()).isEqualTo(vendorId),
                () -> assertThat(result.nodes().get(3).type()).isEqualTo(NodeType.VENDOR),
                () -> assertThat(result.nodes().get(3).name()).isEqualTo("대구업체"),
                () -> assertThat(result.nodes().get(3).sequence()).isEqualTo(4)
        );
    }

    @Test
    @DisplayName("vendor 응답이 null이면 예외가 발생한다")
    void getRoute_vendorNull_throwsException() {
        // given
        UUID sourceHubId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        when(vendorClient.getVendor(vendorId)).thenReturn(null);

        // when // then
        assertThatThrownBy(() -> hubRouteService.getRoute(sourceHubId, vendorId))
                .isInstanceOf(VendorNotFoundException.class);
    }

    @Test
    @DisplayName("vendor 응답의 hubId가 null이면 예외가 발생한다")
    void getRoute_vendorHubIdNull_throwsException() {
        // given
        UUID sourceHubId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        VendorResponse vendorResponse = new VendorResponse(
                vendorId,
                "허브없는업체",
                VendorType.BUYER,
                "설명",
                null,
                "사장님",
                "주소",
                0.0,
                0.0
        );

        when(vendorClient.getVendor(vendorId)).thenReturn(vendorResponse);

        // when // then
        assertThatThrownBy(() -> hubRouteService.getRoute(sourceHubId, vendorId))
                .isInstanceOf(InvalidVendorResponseException.class);
    }
}
