package gift.product.usecase;

import gift.product.dto.ProductResponse;

import java.util.Optional;

public interface GetAdminProductUseCase {
    Optional<ProductResponse> execute(Long id);
}
