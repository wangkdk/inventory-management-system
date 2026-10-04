package inventory.storage.db.core.product;

import inventory.domain.product.Product;
import inventory.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;
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

    @Override
    @Transactional
    public boolean insertIfAbsent(Product product) {
        return productJpaRepository.insertIfAbsent(product.getSku(), product.getName()) == 1;
    }
}
