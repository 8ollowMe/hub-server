package followMe.hub_server.hub.application.dto.result;

import java.util.List;

/** 허브 경로 캐시용 응답에는 사용되지 않음 */
public record HubPathResult(List<RouteNodeResult> hubNodes) {}
