package inventory.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductStockTest {

    @Test
    @DisplayName("재고 수량은 0일 수 있다")
    void quantityCanBeZero() {
        ProductStock stock = ProductStock.of(1L, 0);

        assertThat(stock.getQuantity()).isZero();
    }

    @Test
    @DisplayName("재고 수량은 음수가 될 수 없다")
    void quantityCannotBeNegative() {
        assertThatThrownBy(() -> ProductStock.of(1L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
