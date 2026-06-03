package gift.product.usecase;

import gift.product.entity.Product;

public interface CreateAdminProductUseCase {
    Product execute(String name, int price, String imageUrl, Long categoryId);
}
