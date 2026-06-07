package gift.order.service;

public record OrderCreatedEvent(
    String kakaoAccessToken,
    KakaoOrderMessage message
) {
}
