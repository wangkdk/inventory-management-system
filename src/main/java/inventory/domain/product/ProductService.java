package inventory.domain.product;

import inventory.storage.db.core.product.ProductEntity;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductJpaRepository productJpaRepository;

    public ProductService(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    /**
     * SKU가 등록되어 있지 않으면 상품을 등록한다. 이미 등록된 SKU면 요청의 상품명은 쓰지 않고 기존 상품을 돌려준다.
     */
    @Transactional
    public ProductRegistration registerIfAbsent(String sku, String name) {
        Product product = Product.register(sku, name);
        boolean newlyRegistered = productJpaRepository.insertIfAbsent(product.getSku(), product.getName()) == 1;
        Product registered = productJpaRepository.findBySku(product.getSku())
                .map(ProductService::toProduct)
                .orElseThrow(() -> new IllegalStateException(
                        "동시 등록 뒤 조회 실패: 새로 넣었거나 이미 등록된 SKU인데 상품을 찾지 못했습니다. sku=" + product.getSku()));
        return new ProductRegistration(registered, newlyRegistered);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productJpaRepository.findById(id)
                .map(ProductService::toProduct)
                .orElseThrow(() -> ProductNotFoundException.byId(id));
    }

    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        String normalizedSku = Product.normalizeSku(sku);
        return productJpaRepository.findBySku(normalizedSku)
                .map(ProductService::toProduct)
                .orElseThrow(() -> ProductNotFoundException.bySku(normalizedSku));
    }

    private static Product toProduct(ProductEntity entity) {
        return Product.of(entity.getId(), entity.getSku(), entity.getName());
    }
}
