package followMe.hub_server.hub.presentation.controller;

import com.followMe.common.pagination.PageRequest;
import com.followMe.common.response.ApiResponse;
import followMe.hub_server.hub.application.dto.command.CreateHubCommand;
import followMe.hub_server.hub.application.dto.command.GetHubsQuery;
import followMe.hub_server.hub.application.dto.command.UpdateHubCommand;
import followMe.hub_server.hub.application.dto.result.GetHubsPageResult;
import followMe.hub_server.hub.application.dto.result.HubResult;
import followMe.hub_server.hub.application.service.HubService;
import followMe.hub_server.hub.application.service.UserContext;
import followMe.hub_server.hub.application.service.UserRole;
import followMe.hub_server.hub.presentation.dto.request.GetHubsCondition;
import followMe.hub_server.hub.presentation.dto.request.PostHubReqDto;
import followMe.hub_server.hub.presentation.dto.request.UpdateHubReqDto;
import followMe.hub_server.hub.presentation.dto.response.HubPageResDto;
import followMe.hub_server.hub.presentation.dto.response.HubResDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {

  private final HubService hubService;

  @ModelAttribute
  public UserContext userContext(
      @RequestHeader("X-User-Id") UUID userId,
      @RequestHeader("X-User-Role") UserRole role,
      @RequestHeader(value = "X-Hub-Id", required = false) UUID hubId,
      @RequestHeader(value = "X-Vendor-Id", required = false) UUID vendorId,
      @RequestHeader(value = "X-User-Name", required = false) String userName) {
    return new UserContext(userId, role, hubId, vendorId, userName);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ApiResponse> createHub(
      @ModelAttribute UserContext authUser, @Valid @RequestBody PostHubReqDto reqDto) {
    CreateHubCommand command = reqDto.toCommand();
    HubResult result = hubService.createHub(authUser, command);
    return ApiResponse.created(HubResDto.from(result));
  }

  @GetMapping("/{hubId}")
  public ResponseEntity<ApiResponse> getHub(@PathVariable UUID hubId) {
    HubResult result = hubService.getHub(hubId);
    return ApiResponse.ok(HubResDto.from(result));
  }

  @GetMapping
  public ResponseEntity<ApiResponse> searchHubs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @ModelAttribute GetHubsCondition condition) {
    PageRequest pageRequest = PageRequest.of(page, size);
    GetHubsQuery query = new GetHubsQuery(condition.keyword(), pageRequest);
    GetHubsPageResult result = hubService.searchHubs(query);
    return ApiResponse.ok(HubPageResDto.from(result));
  }

  @PatchMapping("/{hubId}")
  public ResponseEntity<ApiResponse> updateHub(
      @ModelAttribute UserContext authUser,
      @PathVariable UUID hubId,
      @Valid @RequestBody UpdateHubReqDto reqDto) {
    UpdateHubCommand command = reqDto.toCommand(hubId);
    HubResult result = hubService.updateHub(authUser, command);
    return ApiResponse.ok(HubResDto.from(result));
  }

  @DeleteMapping("/{hubId}")
  public ResponseEntity<ApiResponse> deleteHub(
      @ModelAttribute UserContext authUser, @PathVariable UUID hubId) {
    HubResult result = hubService.deleteHub(authUser, hubId);
    return ApiResponse.ok(HubResDto.from(result));
  }
}
