package gift.product.vo;

import java.util.Objects;

public class ProductPrice {
    private final int value;

    public ProductPrice(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("상품 가격은 1 이상이어야 합니다.");
        }
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductPrice that)) {
            return false;
        }
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
