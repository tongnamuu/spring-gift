package gift.order;

public interface KakaoMessageSender {
    void sendToMe(String accessToken, KakaoOrderMessage message);
}
