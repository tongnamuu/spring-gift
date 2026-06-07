package gift.product.usecase;

import gift.product.entity.OptionName;

public record OptionCommand(
    OptionName name,
    int quantity
) {
}
