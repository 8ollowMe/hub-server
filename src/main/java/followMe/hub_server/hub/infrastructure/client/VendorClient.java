package followMe.hub_server.hub.infrastructure.client;

import followMe.hub_server.hub.infrastructure.client.config.VendorClientFeignConfig;
import followMe.hub_server.hub.infrastructure.client.dto.VendorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "vendor-server",
        configuration = VendorClientFeignConfig.class
)
public interface VendorClient {

    @GetMapping("/internal/v1/vendors/{vendorId}")
    VendorResponse getVendor(@PathVariable UUID vendorId);
}
