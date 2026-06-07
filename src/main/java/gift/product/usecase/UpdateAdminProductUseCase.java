package gift.product.usecase;

import gift.product.dto.ProductCommand;
import gift.product.entity.Product;

public interface UpdateAdminProductUseCase {
    Product execute(Long id, ProductCommand command);
}
