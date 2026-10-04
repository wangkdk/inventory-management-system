package inventory.domain.product;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    Optional<Product> findBySku(String sku);

    /**
     * 같은 SKU가 없을 때만 상품을 넣는다. 같은 SKU를 넣고 있는 다른 트랜잭션이 있으면 그 트랜잭션이 끝날 때까지 기다린다.
     *
     * @return 새로 넣었으면 true, 이미 등록된 SKU면 false
     */
    boolean insertIfAbsent(Product product);
}
