package gift.product.dto;

import gift.product.vo.OptionName;

public record OptionCommand(
    OptionName name,
    int quantity
) {
}
