package inventory.domain.stock;

import inventory.domain.product.Product;
import inventory.domain.product.ProductService;
import inventory.storage.db.core.stock.ProductStockEntity;
import inventory.storage.db.core.stock.ProductStockJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductService productService;
    private final ProductStockJpaRepository productStockJpaRepository;

    public InventoryService(ProductService productService, ProductStockJpaRepository productStockJpaRepository) {
        this.productService = productService;
        this.productStockJpaRepository = productStockJpaRepository;
    }

    @Transactional(readOnly = true)
    public StockStatus getStock(Long productId) {
        return toStockStatus(productService.getProduct(productId));
    }

    @Transactional(readOnly = true)
    public StockStatus getStockBySku(String sku) {
        return toStockStatus(productService.getProductBySku(sku));
    }

    private StockStatus toStockStatus(Product product) {
        ProductStock stock = productStockJpaRepository.findByProductId(product.getId())
                .map(InventoryService::toProductStock)
                .orElseThrow(() -> new IllegalStateException("상품은 있는데 재고가 없습니다. productId=" + product.getId()));
        return new StockStatus(product, stock);
    }

    private static ProductStock toProductStock(ProductStockEntity entity) {
        return ProductStock.of(entity.getProductId(), entity.getQuantity());
    }
}
