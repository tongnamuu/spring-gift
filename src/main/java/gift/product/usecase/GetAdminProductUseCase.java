package gift.product.usecase;

import gift.product.entity.Product;

import java.util.Optional;

public interface GetAdminProductUseCase {
    Optional<Product> execute(Long id);
}
