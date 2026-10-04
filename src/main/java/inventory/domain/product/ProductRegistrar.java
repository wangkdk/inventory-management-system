package inventory.domain.product;

import inventory.storage.db.core.product.ProductEntity;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ProductRegistrar {

    private final ProductJpaRepository productJpaRepository;

    public ProductRegistrar(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    /**
     * SKU가 등록되어 있지 않으면 상품을 등록한다. 이미 등록된 SKU면 요청의 상품명은 쓰지 않고 기존 상품을 돌려준다.
     * 대부분 이미 등록된 SKU라 먼저 찾고, 없을 때만 넣는다.
     * 같은 SKU의 첫 입고가 동시에 들어오면 ON CONFLICT가 하나만 넣고, 나머지는 앞 트랜잭션이 끝나길 기다렸다가 다시 찾는다.
     * 다시 찾을 때 앞 트랜잭션이 넣은 행이 보여야 하므로 READ COMMITTED에서만 이렇게 동작한다.
     */
    @Transactional
    public ProductRegistration registerIfAbsent(String sku, String name) {
        String normalizedSku = Product.normalizeSku(sku);
        Optional<Product> registered = findBySku(normalizedSku);
        if (registered.isPresent()) {
            return new ProductRegistration(registered.get(), false);
        }
        Product product = Product.register(normalizedSku, name);
        boolean newlyRegistered = productJpaRepository.insertIfAbsent(product.getSku(), product.getName()) == 1;
        Product saved = findBySku(product.getSku())
                .orElseThrow(() -> new IllegalStateException(
                        "동시 등록 뒤 조회 실패: 새로 넣었거나 이미 등록된 SKU인데 상품을 찾지 못했습니다. sku=" + product.getSku()));
        return new ProductRegistration(saved, newlyRegistered);
    }

    private Optional<Product> findBySku(String sku) {
        return productJpaRepository.findBySku(sku).map(ProductRegistrar::toProduct);
    }

    private static Product toProduct(ProductEntity entity) {
        return Product.of(entity.getId(), entity.getSku(), entity.getName());
    }
}
