package inventory.api.controller.product.dto;

import inventory.domain.stock.StockStatus;

public record ProductStockResponse(Long id, String sku, String name, int quantity) {

    public static ProductStockResponse from(StockStatus status) {
        return new ProductStockResponse(
                status.product().getId(),
                status.product().getSku(),
                status.product().getName(),
                status.stock().getQuantity()
        );
    }
}
