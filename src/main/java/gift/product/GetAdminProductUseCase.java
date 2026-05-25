package gift.product;

import java.util.Optional;

public interface GetAdminProductUseCase {
    Optional<Product> execute(Long id);
}
