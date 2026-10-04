package inventory.domain.product;

import org.springframework.stereotype.Component;

@Component
public class ProductFinder {

    private final ProductRepository productRepository;

    public ProductFinder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ProductNotFoundException.byId(id));
    }

    public Product getProductBySku(String sku) {
        String normalizedSku = Product.normalizeSku(sku);
        return productRepository.findBySku(normalizedSku)
                .orElseThrow(() -> ProductNotFoundException.bySku(normalizedSku));
    }
}
