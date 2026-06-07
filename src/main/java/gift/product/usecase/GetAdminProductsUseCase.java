package gift.product.usecase;

import gift.product.dto.ProductResponse;

import java.util.List;

public interface GetAdminProductsUseCase {
    List<ProductResponse> execute();
}
