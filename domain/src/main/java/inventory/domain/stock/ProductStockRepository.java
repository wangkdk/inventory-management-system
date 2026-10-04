package inventory.domain.stock;

import java.util.Optional;

public interface ProductStockRepository {

    Optional<ProductStock> findByProductId(Long productId);

    /**
     * 재고를 잠그고 찾는다. 같은 상품을 입출고하는 다른 트랜잭션은 이 잠금이 풀릴 때까지 기다린다.
     * 잠금은 트랜잭션이 끝날 때 풀린다. 정해진 시간 안에 잠금을 얻지 못하면 StockLockTimeoutException을 던진다.
     */
    Optional<ProductStock> findByProductIdForUpdate(Long productId);

    void save(ProductStock stock);

    void update(ProductStock stock);
}
