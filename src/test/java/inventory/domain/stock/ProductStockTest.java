package inventory.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    @DisplayName("새 재고는 수량 0으로 만든다")
    void createStartsAtZero() {
        ProductStock stock = ProductStock.create(1L);

        assertThat(stock.getQuantity()).isZero();
    }

    @Test
    @DisplayName("입고하면 수량을 늘린 새 객체를 돌려주고, 원래 객체는 바뀌지 않는다")
    void inboundReturnsIncreasedStock() {
        ProductStock stock = ProductStock.of(1L, 3);

        ProductStock inbounded = stock.inbound(2);

        assertThat(inbounded.getQuantity()).isEqualTo(5);
        assertThat(stock.getQuantity()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("입고 수량은 1 이상이어야 한다")
    void inboundRejectsNonPositiveQuantity(int quantity) {
        ProductStock stock = ProductStock.of(1L, 3);

        assertThatThrownBy(() -> stock.inbound(quantity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("입고 뒤 수량이 int 범위를 넘으면 입고할 수 없다")
    void inboundRejectsOverflow() {
        ProductStock stock = ProductStock.of(1L, Integer.MAX_VALUE);

        assertThatThrownBy(() -> stock.inbound(1))
                .isInstanceOf(ArithmeticException.class);
    }
}
