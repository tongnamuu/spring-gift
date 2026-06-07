package gift.order;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMessageClient {
    private final RestClient restClient;

    public KakaoMessageClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void sendToMe(String accessToken, Order order) {
        var templateObject = buildTemplate(order);

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

    private String buildTemplate(Order order) {
        var totalPrice = String.format("%,d", order.getUnitPrice() * order.getQuantity());
        var message = order.getMessage() != null && !order.getMessage().isBlank()
            ? "\\n\\n💌 " + order.getMessage()
            : "";
        return """
            {
                "object_type": "text",
                "text": "🎁 선물이 도착했어요!\\n\\n%s (%s)\\n수량: %d개\\n금액: %s원%s",
                "link": {},
                "button_title": "선물 확인하기"
            }
            """.formatted(
            order.getProductName(),
            order.getOptionName(),
            order.getQuantity(),
            totalPrice,
            message
        );
    }
}
