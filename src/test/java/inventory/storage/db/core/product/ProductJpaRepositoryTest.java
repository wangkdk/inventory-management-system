package inventory.storage.db.core.product;

import inventory.DbContextTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DbContextTest
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("등록되지 않은 SKU면 상품을 넣고, 시각은 스키마 기본값으로 채워진다")
    void insertIfAbsentInsertsNewSku() {
        int inserted = productJpaRepository.insertIfAbsent("SKU-001", "콜라");

        assertThat(inserted).isEqualTo(1);
        ProductEntity saved = productJpaRepository.findBySku("SKU-001").orElseThrow();
        assertThat(saved.getName()).isEqualTo("콜라");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 등록된 SKU면 넣지 않고 기존 상품명을 유지한다")
    void insertIfAbsentIgnoresRegisteredSku() {
        productJpaRepository.insertIfAbsent("SKU-001", "콜라");

        int inserted = productJpaRepository.insertIfAbsent("SKU-001", "사이다");

        assertThat(inserted).isZero();
        assertThat(productJpaRepository.count()).isEqualTo(1);
        assertThat(productJpaRepository.findBySku("SKU-001").orElseThrow().getName()).isEqualTo("콜라");
    }
}
