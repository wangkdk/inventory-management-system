package inventory.storage.db.core.stock;

import inventory.domain.stock.ProductStock;
import inventory.domain.stock.ProductStockRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class ProductStockRepositoryAdapter implements ProductStockRepository {

    private final ProductStockJpaRepository productStockJpaRepository;

    public ProductStockRepositoryAdapter(ProductStockJpaRepository productStockJpaRepository) {
        this.productStockJpaRepository = productStockJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductStock> findByProductId(Long productId) {
        return productStockJpaRepository.findByProductId(productId).map(ProductStockEntity::toProductStock);
    }

    /**
     * 잠금은 부른 쪽 트랜잭션이 끝날 때 풀린다. 그래서 서비스 트랜잭션 안에서만 부를 수 있고, 트랜잭션 없이 부르면 예외가 난다.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ProductStock> findByProductIdForUpdate(Long productId) {
        return productStockJpaRepository.findByProductIdForUpdate(productId).map(ProductStockEntity::toProductStock);
    }

    @Override
    @Transactional
    public void save(ProductStock stock) {
        productStockJpaRepository.save(ProductStockEntity.of(stock));
    }

    /**
     * 엔티티를 다시 찾아 수량을 바꾼다. 같은 트랜잭션에서 잠그고 읽은 엔티티가 그대로 돌아오고, 바뀐 수량은 커밋할 때 반영된다.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(ProductStock stock) {
        ProductStockEntity entity = productStockJpaRepository.findByProductId(stock.getProductId())
                .orElseThrow(() -> new IllegalStateException("재고가 없습니다. productId=" + stock.getProductId()));
        entity.updateQuantity(stock.getQuantity());
    }
}
