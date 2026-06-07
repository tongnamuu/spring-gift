package gift.product.usecase;

import gift.product.dto.ProductCommand;
import gift.product.entity.Product;

public interface CreateAdminProductUseCase {
    Product execute(ProductCommand command);
}
