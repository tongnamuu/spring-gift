package gift.wish.controller;

import gift.wish.domain.Wish;

public record WishResponse(
    Long id,
    Long productId,
    String name,
    int price,
    String imageUrl
) {
    public static WishResponse from(Wish wish, String productName, int productPrice, String productImageUrl) {
        return new WishResponse(
            wish.getId(),
            wish.getProductId(),
            productName,
            productPrice,
            productImageUrl
        );
    }
}
