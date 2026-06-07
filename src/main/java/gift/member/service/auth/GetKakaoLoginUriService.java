package gift.member.service.auth;

import gift.member.auth.KakaoLoginProperties;
import gift.member.usecase.auth.GetKakaoLoginUriUseCase;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class GetKakaoLoginUriService implements GetKakaoLoginUriUseCase {
    private final KakaoLoginProperties properties;

    public GetKakaoLoginUriService(KakaoLoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public URI execute() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUri();
    }
}
