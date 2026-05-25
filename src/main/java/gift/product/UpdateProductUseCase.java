package gift.product;

import java.util.Optional;

public interface UpdateProductUseCase {
    Optional<ProductResponse> execute(Long id, ProductRequest request);
}
