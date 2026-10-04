package inventory.storage.db.core.stock;

import inventory.storage.db.core.DbContextTest;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DbContextTest
class ProductStockJpaRepositoryTest {

    @Autowired
    private ProductStockJpaRepository productStockJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("상품 id로 그 상품의 재고를 찾는다")
    void findByProductIdReturnsStock() {
        Long productId = insertProduct("SKU-001");
        insertStock(productId, 7);

        ProductStockEntity stock = productStockJpaRepository.findByProductId(productId).orElseThrow();

        assertThat(stock.getProductId()).isEqualTo(productId);
        assertThat(stock.getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고가 없는 상품이면 빈 값을 돌려준다")
    void findByProductIdReturnsEmptyWithoutStock() {
        Long productId = insertProduct("SKU-001");

        Optional<ProductStockEntity> stock = productStockJpaRepository.findByProductId(productId);

        assertThat(stock).isEmpty();
    }

    @Test
    @DisplayName("재고 행을 잠그면서 상품 id로 찾는다")
    void findByProductIdForUpdateReturnsStock() {
        Long productId = insertProduct("SKU-001");
        insertStock(productId, 7);

        ProductStockEntity stock = productStockJpaRepository.findByProductIdForUpdate(productId).orElseThrow();

        assertThat(stock.getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("한 상품에 재고 행을 두 개 만들 수 없다")
    void productIdIsUnique() {
        Long productId = insertProduct("SKU-001");
        insertStock(productId, 0);

        assertThatThrownBy(() -> insertStock(productId, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertProduct(String sku) {
        productJpaRepository.insertIfAbsent(sku, "콜라");
        return productJpaRepository.findBySku(sku).orElseThrow().getId();
    }

    private void insertStock(Long productId, int quantity) {
        productStockJpaRepository.saveAndFlush(new ProductStockEntity(productId, quantity));
    }
}
