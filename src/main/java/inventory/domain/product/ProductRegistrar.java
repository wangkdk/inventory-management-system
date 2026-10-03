package inventory.domain.product;

import inventory.storage.db.core.product.ProductEntity;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductRegistrar {

    private final ProductJpaRepository productJpaRepository;

    public ProductRegistrar(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    /**
     * SKU가 등록되어 있지 않으면 상품을 등록한다. 이미 등록된 SKU면 요청의 상품명은 쓰지 않고 기존 상품을 돌려준다.
     * insertIfAbsent는 @Modifying 쿼리라 트랜잭션 안에서만 돈다. 서비스 트랜잭션 안에서 부르면 거기에 합류한다.
     */
    @Transactional
    public ProductRegistration registerIfAbsent(String sku, String name) {
        Product product = Product.register(sku, name);
        boolean newlyRegistered = productJpaRepository.insertIfAbsent(product.getSku(), product.getName()) == 1;
        Product registered = productJpaRepository.findBySku(product.getSku())
                .map(ProductRegistrar::toProduct)
                .orElseThrow(() -> new IllegalStateException(
                        "동시 등록 뒤 조회 실패: 새로 넣었거나 이미 등록된 SKU인데 상품을 찾지 못했습니다. sku=" + product.getSku()));
        return new ProductRegistration(registered, newlyRegistered);
    }

    private static Product toProduct(ProductEntity entity) {
        return Product.of(entity.getId(), entity.getSku(), entity.getName());
    }
}
