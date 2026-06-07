package gift.product.usecase;

import gift.product.dto.ProductResponse;

import java.util.Optional;

public interface UpdateProductUseCase {
    Optional<ProductResponse> execute(Long id, ProductCommand command);
}
