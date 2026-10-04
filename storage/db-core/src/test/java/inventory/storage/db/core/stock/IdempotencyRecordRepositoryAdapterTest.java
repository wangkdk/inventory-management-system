package inventory.storage.db.core.stock;

import inventory.domain.stock.IdempotencyKeyMismatchException;
import inventory.domain.stock.IdempotencyRecord;
import inventory.domain.stock.MovementType;
import inventory.storage.db.core.DbContextTest;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DbContextTest
@Import(IdempotencyRecordRepositoryAdapter.class)
class IdempotencyRecordRepositoryAdapterTest {

    @Autowired
    private IdempotencyRecordRepositoryAdapter idempotencyRecordRepositoryAdapter;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("같은 요청 키의 기록을 다시 저장하면 IdempotencyKeyMismatchException을 던진다")
    void saveWithDuplicateKeyThrowsMismatch() {
        Long firstProductId = insertProduct("SKU-001");
        Long secondProductId = insertProduct("SKU-002");
        idempotencyRecordRepositoryAdapter.save(
                IdempotencyRecord.of("key-1", firstProductId, MovementType.OUTBOUND, 3, 7));

        assertThatThrownBy(() -> idempotencyRecordRepositoryAdapter.save(
                IdempotencyRecord.of("key-1", secondProductId, MovementType.OUTBOUND, 3, 7)))
                .isInstanceOf(IdempotencyKeyMismatchException.class);
    }

    private Long insertProduct(String sku) {
        productJpaRepository.insertIfAbsent(sku, "콜라");
        return productJpaRepository.findBySku(sku).orElseThrow().getId();
    }
}
