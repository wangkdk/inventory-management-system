package inventory.storage.db.core.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findBySku(String sku);

    /**
     * 같은 SKU의 첫 입고가 동시에 들어와도 상품은 하나만 생긴다. 잠글 행이 아직 없으므로 유니크 제약으로 막는다.
     * 네이티브 쿼리라 BaseEntity의 시각이 채워지지 않으므로, 생성 시각과 수정 시각은 스키마의 DEFAULT now()가 채운다.
     *
     * @return 새로 넣었으면 1, 이미 등록된 SKU면 0
     *
     */
    @Modifying
    @Query(value = """
            INSERT INTO product (sku, name)
            VALUES (:sku, :name)
            ON CONFLICT (sku) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("sku") String sku, @Param("name") String name);
}
