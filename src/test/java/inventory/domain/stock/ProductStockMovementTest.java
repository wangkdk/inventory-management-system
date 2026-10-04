package inventory.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductStockMovementTest {

    @Test
    @DisplayName("입출고를 기록한다")
    void recordKeepsValues() {
        ProductStockMovement movement = ProductStockMovement.record(1L, MovementType.INBOUND, 5, 12);

        assertThat(movement.getProductId()).isEqualTo(1L);
        assertThat(movement.getType()).isEqualTo(MovementType.INBOUND);
        assertThat(movement.getQuantity()).isEqualTo(5);
        assertThat(movement.getQuantityAfter()).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("변동 수량은 1 이상이어야 한다")
    void recordRejectsNonPositiveQuantity(int quantity) {
        assertThatThrownBy(() -> ProductStockMovement.record(1L, MovementType.INBOUND, quantity, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("변동 뒤 재고 수량은 음수가 될 수 없다")
    void recordRejectsNegativeQuantityAfter() {
        assertThatThrownBy(() -> ProductStockMovement.record(1L, MovementType.OUTBOUND, 1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
