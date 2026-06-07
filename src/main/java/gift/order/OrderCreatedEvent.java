package gift.order;

public record OrderCreatedEvent(
    String kakaoAccessToken,
    KakaoOrderMessage message
) {
}
