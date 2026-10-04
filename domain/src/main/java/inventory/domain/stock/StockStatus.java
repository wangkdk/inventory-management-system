package inventory.domain.stock;

import inventory.domain.product.Product;

public record StockStatus(Product product, ProductStock stock) {
}
