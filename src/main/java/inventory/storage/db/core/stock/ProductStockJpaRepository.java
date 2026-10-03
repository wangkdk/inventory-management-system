package inventory.storage.db.core.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductStockJpaRepository extends JpaRepository<ProductStockEntity, Long> {

    Optional<ProductStockEntity> findByProductId(Long productId);
}
