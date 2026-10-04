package inventory.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyRecordTest {

    @Test
    @DisplayName("상품, 유형, 수량이 모두 같아야 이 기록을 남긴 요청과 같은 요청이다")
    void isSameRequestComparesProductTypeAndQuantity() {
        IdempotencyRecord processed = IdempotencyRecord.of("key-1", 1L, MovementType.OUTBOUND, 3, 7);

        assertThat(processed.isSameRequest(1L, MovementType.OUTBOUND, 3)).isTrue();
        assertThat(processed.isSameRequest(2L, MovementType.OUTBOUND, 3)).isFalse();
        assertThat(processed.isSameRequest(1L, MovementType.INBOUND, 3)).isFalse();
        assertThat(processed.isSameRequest(1L, MovementType.OUTBOUND, 4)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("요청 키는 비어 있을 수 없다")
    void ofRejectsBlankKey(String idempotencyKey) {
        assertThatThrownBy(() -> IdempotencyRecord.of(idempotencyKey, 1L, MovementType.OUTBOUND, 3, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("변동 수량은 1 이상이어야 한다")
    void ofRejectsNonPositiveQuantity(int quantity) {
        assertThatThrownBy(() -> IdempotencyRecord.of("key-1", 1L, MovementType.INBOUND, quantity, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("변동 뒤 재고 수량은 음수가 될 수 없다")
    void ofRejectsNegativeQuantityAfter() {
        assertThatThrownBy(() -> IdempotencyRecord.of("key-1", 1L, MovementType.OUTBOUND, 1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
