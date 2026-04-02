package followMe.hub_server.hub.application.service;

import java.util.UUID;

public record UserContext(UUID userId, UserRole role, UUID hubId, UUID vendorId, String userName) {}
