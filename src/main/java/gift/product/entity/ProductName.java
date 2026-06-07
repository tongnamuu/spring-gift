package gift.product.entity;

import gift.product.validator.ProductNameValidator;

import java.util.List;
import java.util.Objects;

public class ProductName {
    private final String value;

    public ProductName(String value) {
        this(value, false);
    }

    public static ProductName allowingKakao(String value) {
        return new ProductName(value, true);
    }

    private ProductName(String value, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(value, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductName that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
