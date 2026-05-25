package gift.auth;

public interface LoginWithKakaoAuthorizationCodeUseCase {
    TokenResponse execute(String code);
}
