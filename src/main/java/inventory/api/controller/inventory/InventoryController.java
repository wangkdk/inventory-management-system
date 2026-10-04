package inventory.api.controller.inventory;

import inventory.api.controller.inventory.dto.InboundRequest;
import inventory.api.controller.inventory.dto.OutboundRequest;
import inventory.api.controller.product.dto.ProductStockResponse;
import inventory.domain.stock.InventoryService;
import inventory.domain.stock.StockStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inbound")
    public ProductStockResponse inbound(@RequestBody @Valid InboundRequest request) {
        StockStatus status = inventoryService.inbound(request.toInboundItem());
        return ProductStockResponse.from(status);
    }

    @PostMapping("/outbound")
    public ProductStockResponse outbound(@RequestBody @Valid OutboundRequest request) {
        StockStatus status = inventoryService.outbound(request.toOutboundItem());
        return ProductStockResponse.from(status);
    }
}
