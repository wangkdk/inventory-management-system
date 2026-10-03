package inventory.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("상품을 등록하면 SKU의 앞뒤 공백을 제거한다")
    void registerStripsSku() {
        Product product = Product.register("  SKU-001 ", "콜라");

        assertThat(product.getId()).isNull();
        assertThat(product.getSku()).isEqualTo("SKU-001");
        assertThat(product.getName()).isEqualTo("콜라");
    }

    @Test
    @DisplayName("SKU는 대소문자를 바꾸지 않는다")
    void registerKeepsSkuCase() {
        Product product = Product.register("sku-001", "콜라");

        assertThat(product.getSku()).isEqualTo("sku-001");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("SKU가 비어 있으면 등록할 수 없다")
    void registerFailsWhenSkuIsBlank(String sku) {
        assertThatThrownBy(() -> Product.register(sku, "콜라"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("상품명이 비어 있으면 등록할 수 없다")
    void registerFailsWhenNameIsBlank(String name) {
        assertThatThrownBy(() -> Product.register("SKU-001", name))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
