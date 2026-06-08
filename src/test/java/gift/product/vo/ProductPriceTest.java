package gift.product.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPriceTest {
    @Test
    void productPriceAllowsPositiveValue() {
        ProductPrice price = new ProductPrice(1);

        assertThat(price.value()).isEqualTo(1);
    }

    @Test
    void productPriceRejectsZeroValue() {
        assertThatThrownBy(() -> new ProductPrice(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 가격은 1 이상이어야 합니다.");
    }
}
