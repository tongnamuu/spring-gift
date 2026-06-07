package gift.order.service;

public interface KakaoMessageSender {
    void sendToMe(String accessToken, KakaoOrderMessage message);
}
