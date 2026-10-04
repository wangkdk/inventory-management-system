package inventory.storage.db.core.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductStockJpaRepository extends JpaRepository<ProductStockEntity, Long> {

    Optional<ProductStockEntity> findByProductId(Long productId);

    /**
     * 재고 행을 잠그고 찾는다. 같은 상품을 입출고하는 다른 트랜잭션은 이 잠금이 풀릴 때까지 기다린다.
     * 잠금은 트랜잭션이 끝날 때 풀린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ProductStockEntity s where s.productId = :productId")
    Optional<ProductStockEntity> findByProductIdForUpdate(@Param("productId") Long productId);
}
