package inventory.storage.db.core.stock;

import inventory.DbContextTest;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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

    @Test
    @DisplayName("입출고 기록을 저장한다")
    void saveMovement() {
        Long productId = insertProduct("SKU-001");

        ProductStockMovementEntity saved = productStockMovementJpaRepository.saveAndFlush(
                new ProductStockMovementEntity(productId, "INBOUND", 5, 5));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("입고와 출고가 아닌 유형은 저장할 수 없다")
    void typeMustBeInboundOrOutbound() {
        Long productId = insertProduct("SKU-001");

        assertThatThrownBy(() -> productStockMovementJpaRepository.saveAndFlush(
                new ProductStockMovementEntity(productId, "TRANSFER", 5, 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("변동 수량이 1보다 작은 기록은 저장할 수 없다")
    void quantityMustBePositive() {
        Long productId = insertProduct("SKU-001");

        assertThatThrownBy(() -> productStockMovementJpaRepository.saveAndFlush(
                new ProductStockMovementEntity(productId, "INBOUND", 0, 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertProduct(String sku) {
        productJpaRepository.insertIfAbsent(sku, "콜라");
        return productJpaRepository.findBySku(sku).orElseThrow().getId();
    }
}
