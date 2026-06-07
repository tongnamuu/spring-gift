package gift.order.service;

import gift.order.domain.Order;

public record KakaoOrderMessage(
    String productName,
    String optionName,
    int unitPrice,
    int quantity,
    String message
) {
    public static KakaoOrderMessage from(Order order) {
        return new KakaoOrderMessage(
            order.getProductName(),
            order.getOptionName(),
            order.getUnitPrice(),
            order.getQuantity(),
            order.getMessage()
        );
    }
}
