package gift.order.service;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient implements KakaoMessageSender {
    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public void sendToMe(String accessToken, KakaoOrderMessage message) {
        var templateObject = buildTemplate(message);

        var params = new LinkedMultiValueMap<String, String>();
        params.add("template_object", templateObject);

        restClient.post()
            .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(params)
            .retrieve()
            .toBodilessEntity();
    }

    private String buildTemplate(KakaoOrderMessage message) {
        var totalPrice = String.format("%,d", message.unitPrice() * message.quantity());
        var giftMessage = message.message() != null && !message.message().isBlank()
            ? "\\n\\n💌 " + message.message()
            : "";
        return """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %d개\\n금액: %s원%s",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """.formatted(
            message.productName(),
            message.optionName(),
            message.quantity(),
            totalPrice,
            giftMessage
        );
    }
}
