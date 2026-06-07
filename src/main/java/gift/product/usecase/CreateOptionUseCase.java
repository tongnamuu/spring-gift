package gift.product.usecase;

import gift.product.dto.OptionCommand;
import gift.product.dto.OptionResponse;

public interface CreateOptionUseCase {
    OptionResponse execute(Long productId, OptionCommand command);
}
