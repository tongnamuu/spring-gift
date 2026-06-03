package gift.product.usecase;

import gift.product.entity.Product;

import java.util.List;

public interface GetAdminProductsUseCase {
    List<Product> execute();
}
