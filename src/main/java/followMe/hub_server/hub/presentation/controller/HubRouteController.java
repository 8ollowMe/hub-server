package followMe.hub_server.hub.presentation.controller;

import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import followMe.hub_server.hub.application.service.HubRouteService;
import followMe.hub_server.hub.presentation.dto.response.HubRouteResDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @GetMapping("/route")
    public HubRouteResDto getRoute(
            @RequestParam UUID sourceHubId,
            @RequestParam UUID vendorId) {
        HubRouteResult result = hubRouteService.getRoute(sourceHubId, vendorId);
        return HubRouteResDto.from(result);
    }
}
