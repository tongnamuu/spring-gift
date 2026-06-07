package gift.member.usecase.auth;

import gift.member.auth.TokenResponse;

public interface LoginWithKakaoAuthorizationCodeUseCase {
    TokenResponse execute(KakaoAuthorizationCodeCommand command);
}
