package gift.product.usecase;

import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;

import java.util.Optional;

public interface UpdateProductUseCase {
    Optional<ProductResponse> execute(Long id, ProductRequest request);
}
