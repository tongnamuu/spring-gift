package gift.order.usecase;

public record OrderCommand(
    Long optionId,
    int quantity,
    String message
) {
}
