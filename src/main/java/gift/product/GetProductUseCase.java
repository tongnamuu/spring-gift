package gift.product;

import java.util.Optional;

public interface GetProductUseCase {
    Optional<ProductResponse> execute(Long id);
}
