package inventory.storage.db.core.stock;

import inventory.DbContextTest;
import inventory.storage.db.core.product.ProductJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DbContextTest
class ProductStockJpaRepositoryTest {

    @Autowired
    private ProductStockJpaRepository productStockJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private Long insertProduct(String sku) {
        productJpaRepository.insertIfAbsent(sku, "콜라");
        return productJpaRepository.findBySku(sku).orElseThrow().getId();
    }

    /**
     * 재고 행을 만드는 코드는 입고 기능에서 생긴다. 그 전까지는 SQL로 직접 넣는다.
     */
    private void insertStock(Long productId, int quantity) {
        jdbcTemplate.update("INSERT INTO product_stock (product_id, quantity) VALUES (?, ?)", productId, quantity);
    }
}
