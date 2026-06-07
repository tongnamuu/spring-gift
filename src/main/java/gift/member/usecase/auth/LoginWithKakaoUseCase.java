package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;
import gift.member.dto.KakaoAuthorizationCodeCommand;

public interface LoginWithKakaoUseCase {
    TokenResponse execute(KakaoAuthorizationCodeCommand command);
}
