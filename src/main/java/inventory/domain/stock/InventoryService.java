package inventory.domain.stock;

import inventory.domain.product.Product;
import inventory.domain.product.ProductFinder;
import inventory.storage.db.core.stock.ProductStockEntity;
import inventory.storage.db.core.stock.ProductStockJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductFinder productFinder;
    private final ProductStockJpaRepository productStockJpaRepository;

    public InventoryService(ProductFinder productFinder, ProductStockJpaRepository productStockJpaRepository) {
        this.productFinder = productFinder;
        this.productStockJpaRepository = productStockJpaRepository;
    }

    @Transactional(readOnly = true)
    public StockStatus getStock(Long productId) {
        return toStockStatus(productFinder.getProduct(productId));
    }

    @Transactional(readOnly = true)
    public StockStatus getStockBySku(String sku) {
        return toStockStatus(productFinder.getProductBySku(sku));
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
