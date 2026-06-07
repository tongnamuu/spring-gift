package gift.member.auth;

import gift.member.usecase.auth.GetKakaoLoginUriUseCase;
import gift.member.usecase.auth.KakaoAuthorizationCodeCommand;
import gift.member.usecase.auth.LoginWithKakaoAuthorizationCodeUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Handles the Kakao OAuth2 login flow.
 * 1. /login redirects the user to Kakao's authorization page
 * 2. /callback receives the authorization code, exchanges it for an access token,
 *    retrieves user info, auto-registers the member if new, and issues a service JWT
 */
@RestController
@RequestMapping(path = "/api/auth/kakao")
public class KakaoAuthController {
    private final GetKakaoLoginUriUseCase getKakaoLoginUriUseCase;
    private final LoginWithKakaoAuthorizationCodeUseCase loginWithKakaoAuthorizationCodeUseCase;

    public KakaoAuthController(
        GetKakaoLoginUriUseCase getKakaoLoginUriUseCase,
        LoginWithKakaoAuthorizationCodeUseCase loginWithKakaoAuthorizationCodeUseCase
    ) {
        this.getKakaoLoginUriUseCase = getKakaoLoginUriUseCase;
        this.loginWithKakaoAuthorizationCodeUseCase = loginWithKakaoAuthorizationCodeUseCase;
    }

    @GetMapping(path = "/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, getKakaoLoginUriUseCase.execute().toString())
            .build();
    }

    @GetMapping(path = "/callback")
    public ResponseEntity<TokenResponse> callback(@RequestParam("code") String code) {
        return ResponseEntity.ok(loginWithKakaoAuthorizationCodeUseCase.execute(toCommand(code)));
    }

    private KakaoAuthorizationCodeCommand toCommand(String code) {
        return new KakaoAuthorizationCodeCommand(code);
    }
}
