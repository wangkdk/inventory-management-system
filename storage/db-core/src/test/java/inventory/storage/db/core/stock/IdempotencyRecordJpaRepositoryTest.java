package inventory.storage.db.core.stock;

import inventory.domain.stock.MovementType;
import inventory.storage.db.core.DbContextTest;
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
class IdempotencyRecordJpaRepositoryTest {

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("같은 요청 키로 기록을 두 번 저장할 수 없다")
    void idempotencyKeyMustBeUnique() {
        Long productId = insertProduct("SKU-001");
        idempotencyRecordJpaRepository.saveAndFlush(
                new IdempotencyRecordEntity("key-1", productId, MovementType.INBOUND, 5, 5));

        assertThatThrownBy(() -> idempotencyRecordJpaRepository.saveAndFlush(
                new IdempotencyRecordEntity("key-1", productId, MovementType.INBOUND, 5, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("요청 키로 기록을 찾는다")
    void findByIdempotencyKey() {
        Long productId = insertProduct("SKU-001");
        idempotencyRecordJpaRepository.saveAndFlush(
                new IdempotencyRecordEntity("key-1", productId, MovementType.OUTBOUND, 3, 7));

        assertThat(idempotencyRecordJpaRepository.findByIdempotencyKey("key-1"))
                .hasValueSatisfying(found -> assertThat(found.getQuantityAfter()).isEqualTo(7));
        assertThat(idempotencyRecordJpaRepository.findByIdempotencyKey("key-2")).isEmpty();
    }

    private Long insertProduct(String sku) {
        productJpaRepository.insertIfAbsent(sku, "콜라");
        return productJpaRepository.findBySku(sku).orElseThrow().getId();
    }
}
