package gift.product.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPriceTest {
    @Test
    void productPriceAllowsZero() {
        ProductPrice price = new ProductPrice(0);

        assertThat(price.value()).isZero();
    }

    @Test
    void productPriceRejectsNegativeValue() {
        assertThatThrownBy(() -> new ProductPrice(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 가격은 0 이상이어야 합니다.");
    }
}
