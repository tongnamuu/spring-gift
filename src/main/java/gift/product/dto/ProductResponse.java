package gift.product.dto;

import gift.product.entity.Product;

import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    int price,
    String imageUrl,
    Long categoryId,
    List<OptionResponse> options
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getImageUrl(),
            product.getCategoryId(),
            product.getOptions().stream()
                .map(OptionResponse::from)
                .toList()
        );
    }
}
