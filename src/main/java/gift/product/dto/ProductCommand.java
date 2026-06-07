package gift.product.dto;

import gift.product.entity.Product;
import gift.product.vo.ProductName;

public record ProductCommand(
    ProductName name,
    int price,
    String imageUrl,
    Long categoryId
) {
    public Product toEntity() {
        return new Product(name, price, imageUrl, categoryId);
    }
}
