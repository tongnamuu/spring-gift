package gift.product.usecase;

import gift.product.dto.OptionResponse;

import java.util.List;
import java.util.Optional;

public interface GetOptionsUseCase {
    Optional<List<OptionResponse>> execute(Long productId);
}
