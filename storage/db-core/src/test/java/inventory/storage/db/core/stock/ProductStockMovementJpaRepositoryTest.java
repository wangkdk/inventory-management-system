package inventory.storage.db.core.stock;

import inventory.domain.stock.MovementType;
import inventory.storage.db.core.DbContextTest;
import inventory.storage.db.core.product.ProductJpaRepository;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DbContextTest
class ProductStockMovementJpaRepositoryTest {

    @Autowired
    private ProductStockMovementJpaRepository productStockMovementJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("입출고 기록을 저장한다")
    void saveMovement() {
        Long productId = insertProduct("SKU-001");

        ProductStockMovementEntity saved = productStockMovementJpaRepository.saveAndFlush(
                new ProductStockMovementEntity(productId, MovementType.INBOUND, 5, 5));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("입고와 출고가 아닌 유형은 저장할 수 없다")
    void typeMustBeInboundOrOutbound() {
        Long productId = insertProduct("SKU-001");

        // 엔티티는 MovementType만 받아서, DB의 CHECK 제약은 SQL로 직접 넣어 확인한다
        assertThatThrownBy(() -> testEntityManager.getEntityManager()
                .createNativeQuery("""
                        INSERT INTO product_stock_movement (product_id, type, quantity, quantity_after)
                        VALUES (:productId, 'TRANSFER', 5, 5)
                        """)
                .setParameter("productId", productId)
                .executeUpdate())
                .isInstanceOf(PersistenceException.class)
                .rootCause()
                .hasMessageContaining("ck_product_stock_movement_type");
    }

    @Test
    @DisplayName("변동 수량이 1보다 작은 기록은 저장할 수 없다")
    void quantityMustBePositive() {
        Long productId = insertProduct("SKU-001");

        assertThatThrownBy(() -> productStockMovementJpaRepository.saveAndFlush(
                new ProductStockMovementEntity(productId, MovementType.INBOUND, 0, 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertProduct(String sku) {
        productJpaRepository.insertIfAbsent(sku, "콜라");
        return productJpaRepository.findBySku(sku).orElseThrow().getId();
    }
}
