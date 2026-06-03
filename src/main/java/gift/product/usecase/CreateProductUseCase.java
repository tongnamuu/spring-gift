package gift.product.usecase;

import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;

public interface CreateProductUseCase {
    ProductResponse execute(ProductRequest request);
}
