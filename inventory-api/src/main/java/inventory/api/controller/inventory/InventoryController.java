package inventory.api.controller.inventory;

import inventory.api.controller.inventory.dto.InboundRequest;
import inventory.api.controller.inventory.dto.OutboundRequest;
import inventory.api.controller.product.dto.ProductStockResponse;
import inventory.domain.stock.InventoryService;
import inventory.domain.stock.StockStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    /**
     * 요청 키를 담는 헤더. 응답을 받지 못해 다시 보낼 때 같은 값을 쓰면 한 번만 반영된다.
     */
    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inbound")
    public ProductStockResponse inbound(
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) @Size(min = 1, max = 64) String idempotencyKey,
            @RequestBody @Valid InboundRequest request
    ) {
        StockStatus status = inventoryService.inbound(request.toInboundItem(), idempotencyKey);
        return ProductStockResponse.from(status);
    }

    @PostMapping("/outbound")
    public ProductStockResponse outbound(
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) @Size(min = 1, max = 64) String idempotencyKey,
            @RequestBody @Valid OutboundRequest request
    ) {
        StockStatus status = inventoryService.outbound(request.toOutboundItem(), idempotencyKey);
        return ProductStockResponse.from(status);
    }
}
