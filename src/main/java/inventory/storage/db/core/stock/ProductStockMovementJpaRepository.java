package inventory.storage.db.core.stock;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStockMovementJpaRepository extends JpaRepository<ProductStockMovementEntity, Long> {
}
