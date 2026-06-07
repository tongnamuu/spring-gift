package gift.product.usecase;

import gift.product.vo.OptionName;

public record OptionCommand(
    OptionName name,
    int quantity
) {
}
