package inventory.domain.product;

import inventory.storage.db.core.product.ProductEntity;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductFinder {

    private final ProductJpaRepository productJpaRepository;

    public ProductFinder(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    public Product getProduct(Long id) {
        return productJpaRepository.findById(id)
                .map(ProductFinder::toProduct)
                .orElseThrow(() -> ProductNotFoundException.byId(id));
    }

    public Product getProductBySku(String sku) {
        String normalizedSku = Product.normalizeSku(sku);
        return productJpaRepository.findBySku(normalizedSku)
                .map(ProductFinder::toProduct)
                .orElseThrow(() -> ProductNotFoundException.bySku(normalizedSku));
    }

    private static Product toProduct(ProductEntity entity) {
        return Product.of(entity.getId(), entity.getSku(), entity.getName());
    }
}
