package inventory.api.controller.product;

import inventory.api.controller.product.dto.ProductStockResponse;
import inventory.domain.stock.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ProductStockResponse getProduct(@PathVariable Long productId) {
        return ProductStockResponse.from(inventoryService.getStock(productId));
    }

    @GetMapping
    public ProductStockResponse getProductBySku(@RequestParam String sku) {
        return ProductStockResponse.from(inventoryService.getStockBySku(sku));
    }
}
