package gift.product.usecase;

import gift.product.entity.Product;

public interface UpdateAdminProductUseCase {
    Product execute(Long id, String name, int price, String imageUrl, Long categoryId);
}
