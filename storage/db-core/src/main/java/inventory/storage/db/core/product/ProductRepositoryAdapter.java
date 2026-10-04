package inventory.storage.db.core.product;

import inventory.domain.product.Product;
import inventory.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id).map(ProductEntity::toProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return productJpaRepository.findBySku(sku).map(ProductEntity::toProduct);
    }

    /**
     * 상품은 재고 행과 같은 트랜잭션에서 만들어야 해서, 서비스 트랜잭션 안에서만 부를 수 있다.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean insertIfAbsent(Product product) {
        return productJpaRepository.insertIfAbsent(product.getSku(), product.getName()) == 1;
    }
}
