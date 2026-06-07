package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;

public interface LoginWithKakaoUseCase {
    TokenResponse execute(KakaoAuthorizationCodeCommand command);
}
