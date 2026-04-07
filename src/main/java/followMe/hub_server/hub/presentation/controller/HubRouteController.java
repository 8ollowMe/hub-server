package followMe.hub_server.hub.presentation.controller;

import com.followMe.common.pagination.PageRequest;
import com.followMe.common.response.ApiResponse;
import followMe.hub_server.hub.application.dto.command.SearchHubRoutesQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubRouteCommand;
import followMe.hub_server.hub.application.dto.result.HubRouteDetailResult;
import followMe.hub_server.hub.application.dto.result.HubRoutePageResult;
import followMe.hub_server.hub.application.dto.result.HubRouteResult;
import followMe.hub_server.hub.application.service.HubRouteService;
import followMe.hub_server.hub.application.service.UserContext;
import followMe.hub_server.hub.application.service.UserRole;
import followMe.hub_server.hub.presentation.dto.request.PostHubRouteReqDto;
import followMe.hub_server.hub.presentation.dto.request.UpdateHubRouteReqDto;
import followMe.hub_server.hub.presentation.dto.response.HubRouteResDto;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HubRouteController {

  private final HubRouteService hubRouteService;

    @ModelAttribute
    public UserContext userContext(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Role") UserRole role,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID hubId,
            @RequestHeader(value = "X-Vendor-Id", required = false) UUID vendorId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        return new UserContext(userId, role, hubId, vendorId, userName);
    }

    @PostMapping("/api/v1/hub-routes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse> create(
            @ModelAttribute UserContext authUser,
            @Valid @RequestBody PostHubRouteReqDto reqDto) {
        HubRouteDetailResult result = hubRouteService.create(authUser, reqDto.toCommand());
        return ApiResponse.created(result);
    }

    @GetMapping("/api/v1/hub-routes/{hubRouteId}")
    public ResponseEntity<ApiResponse> get(@PathVariable UUID hubRouteId) {
        return ApiResponse.ok(hubRouteService.get(hubRouteId));
    }

    @GetMapping("/api/v1/hub-routes")
    public ResponseEntity<ApiResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UUID originHubId,
            @RequestParam(required = false) UUID destinationHubId) {
        HubRoutePageResult result =
                hubRouteService.search(
                        new SearchHubRoutesQuery(originHubId, destinationHubId, PageRequest.of(page, size)));
        return ApiResponse.ok(result);
    }

    @PatchMapping("/api/v1/hub-routes/{hubRouteId}")
    public ResponseEntity<ApiResponse> update(
            @ModelAttribute UserContext authUser,
            @PathVariable UUID hubRouteId,
            @Valid @RequestBody UpdateHubRouteReqDto reqDto) {
        UpdateHubRouteCommand command = reqDto.toCommand(hubRouteId);
        return ApiResponse.ok(hubRouteService.update(authUser, command));
    }

    @DeleteMapping("/api/v1/hub-routes/{hubRouteId}")
    public ResponseEntity<ApiResponse> delete(
            @ModelAttribute UserContext authUser,
            @PathVariable UUID hubRouteId) {
        return ApiResponse.ok(hubRouteService.delete(authUser, hubRouteId));
    }

  @GetMapping("/api/hubs/route")
  public HubRouteResDto getRoute(@RequestParam UUID sourceHubId, @RequestParam UUID vendorId) {
    HubRouteResult result = hubRouteService.getRoute(sourceHubId, vendorId);
    return HubRouteResDto.from(result);
  }
}
