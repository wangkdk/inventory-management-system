package inventory.storage.db.core.stock;

import inventory.domain.stock.ProductStockMovement;
import inventory.domain.stock.ProductStockMovementRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProductStockMovementRepositoryAdapter implements ProductStockMovementRepository {

    private final ProductStockMovementJpaRepository productStockMovementJpaRepository;

    public ProductStockMovementRepositoryAdapter(ProductStockMovementJpaRepository productStockMovementJpaRepository) {
        this.productStockMovementJpaRepository = productStockMovementJpaRepository;
    }

    @Override
    @Transactional
    public void save(ProductStockMovement movement) {
        productStockMovementJpaRepository.save(ProductStockMovementEntity.of(movement));
    }
}
