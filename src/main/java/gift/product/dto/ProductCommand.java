package gift.product.dto;

import gift.product.entity.Product;
import gift.product.vo.ProductName;
import gift.product.vo.ProductPrice;

public record ProductCommand(
    ProductName name,
    ProductPrice price,
    String imageUrl,
    Long categoryId
) {
    public Product toEntity() {
        return new Product(name, price, imageUrl, categoryId);
    }
}
