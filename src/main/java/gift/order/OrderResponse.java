package gift.order;

import java.time.LocalDateTime;

public record OrderResponse(
    Long id,
    Long productId,
    Long optionId,
    String productName,
    String optionName,
    int unitPrice,
    int totalPrice,
    String productImageUrl,
    int quantity,
    LocalDateTime orderDateTime,
    String message
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getProductId(),
            order.getOptionId(),
            order.getProductName(),
            order.getOptionName(),
            order.getUnitPrice(),
            order.getUnitPrice() * order.getQuantity(),
            order.getProductImageUrl(),
            order.getQuantity(),
            order.getOrderDateTime(),
            order.getMessage()
        );
    }
}
