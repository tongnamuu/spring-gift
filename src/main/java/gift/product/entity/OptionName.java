package gift.product.entity;

import gift.product.validator.OptionNameValidator;

import java.util.List;
import java.util.Objects;

public class OptionName {
    private final String value;

    public OptionName(String value) {
        List<String> errors = OptionNameValidator.validate(value);
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
        if (!(other instanceof OptionName that)) {
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
