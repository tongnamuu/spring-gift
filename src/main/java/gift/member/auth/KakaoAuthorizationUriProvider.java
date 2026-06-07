package gift.member.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class KakaoAuthorizationUriProvider {
    private final KakaoLoginProperties properties;

    public KakaoAuthorizationUriProvider(KakaoLoginProperties properties) {
        this.properties = properties;
    }

    public URI provide() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUri();
    }
}
