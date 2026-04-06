package followMe.hub_server.hub.infrastructure.client.dto;

import followMe.hub_server.hub.infrastructure.client.enums.VendorType;
import java.util.UUID;

public record VendorResponse(
    UUID vendorId,
    String name,
    VendorType type,
    String description,
    UUID hubId,
    String ownerName,
    String address,
    Double latitude,
    Double longitude) {}
