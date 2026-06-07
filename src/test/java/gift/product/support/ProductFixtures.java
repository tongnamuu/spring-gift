package gift.product.support;

import gift.product.entity.Product;
import gift.product.vo.ProductName;
import gift.product.vo.ProductPrice;

public final class ProductFixtures {
    private ProductFixtures() {
    }

    public static Product product(String name, int price, String imageUrl, Long categoryId) {
        return new Product(
            ProductName.allowingKakao(name),
            new ProductPrice(price),
            imageUrl,
            categoryId
        );
    }
}
