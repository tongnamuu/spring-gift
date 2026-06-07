package gift.order.dto;

public record OrderCommand(
    Long optionId,
    int quantity,
    String message
) {
}
