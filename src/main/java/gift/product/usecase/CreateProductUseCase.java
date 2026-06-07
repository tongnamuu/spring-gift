package gift.product.usecase;

import gift.product.dto.ProductCommand;
import gift.product.dto.ProductResponse;

public interface CreateProductUseCase {
    ProductResponse execute(ProductCommand command);
}
