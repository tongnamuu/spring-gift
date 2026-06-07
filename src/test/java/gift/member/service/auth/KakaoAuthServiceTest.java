package gift.member.service.auth;

import gift.member.auth.JwtProvider;
import gift.member.auth.KakaoLoginClient;
import gift.member.auth.TokenResponse;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.usecase.auth.KakaoAuthorizationCodeCommand;
import gift.member.usecase.auth.LoginWithKakaoUseCase;
import gift.support.AbstractMysqlServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Import(KakaoAuthServiceTest.KakaoAuthTestConfig.class)
@TestPropertySource(properties = {
    "kakao.login.client-id=test-client-id",
    "kakao.login.client-secret=test-client-secret",
    "kakao.login.redirect-uri=http://localhost:8080/api/auth/kakao/callback"
})
class KakaoAuthServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_EMAIL_PREFIX = "kakao-service-";

    @Autowired
    private LoginWithKakaoUseCase loginWithKakaoUseCase;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeKakaoLoginClient kakaoLoginClient;

    @BeforeEach
    void setUp() {
        kakaoLoginClient.reset();
        deleteTestMembers();
    }

    @AfterEach
    void tearDown() {
        deleteTestMembers();
        kakaoLoginClient.reset();
    }

    @Test
    void loginWithKakaoCreatesMemberAndReturnsToken() {
        String email = TEST_EMAIL_PREFIX + "new@example.com";
        kakaoLoginClient.prepare("kakao-access-token-new", email);

        TokenResponse response = loginWithKakaoUseCase.execute(
            new KakaoAuthorizationCodeCommand("authorization-code")
        );

        assertThat(jwtProvider.getEmail(response.token())).isEqualTo(email);
        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getPassword()).isNull();
        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-access-token-new");
        assertThat(kakaoLoginClient.requestedCode()).isEqualTo("authorization-code");
        assertThat(kakaoLoginClient.requestedUserInfoAccessToken()).isEqualTo("kakao-access-token-new");
    }

    @Test
    void loginWithKakaoUpdatesExistingMemberToken() {
        String email = TEST_EMAIL_PREFIX + "existing@example.com";
        Member existing = memberRepository.save(new Member(email, "password123"));
        kakaoLoginClient.prepare("kakao-access-token-updated", email);

        TokenResponse response = loginWithKakaoUseCase.execute(
            new KakaoAuthorizationCodeCommand("authorization-code")
        );

        assertThat(jwtProvider.getEmail(response.token())).isEqualTo(email);
        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getId()).isEqualTo(existing.getId());
        assertThat(member.getPassword()).isEqualTo("password123");
        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-access-token-updated");
        assertThat(countMembersByEmail(email)).isEqualTo(1L);
    }

    private long countMembersByEmail(String email) {
        return jdbcTemplate.queryForObject(
            "select count(*) from member where email = ?",
            Long.class,
            email
        );
    }

    private void deleteTestMembers() {
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
    }

    @TestConfiguration
    static class KakaoAuthTestConfig {
        @Bean
        @Primary
        FakeKakaoLoginClient fakeKakaoLoginClient() {
            return new FakeKakaoLoginClient();
        }
    }

    static class FakeKakaoLoginClient implements KakaoLoginClient {
        private String accessToken = "kakao-access-token";
        private String email = TEST_EMAIL_PREFIX + "default@example.com";
        private String requestedCode;
        private String requestedUserInfoAccessToken;

        void prepare(String accessToken, String email) {
            this.accessToken = accessToken;
            this.email = email;
        }

        void reset() {
            accessToken = "kakao-access-token";
            email = TEST_EMAIL_PREFIX + "default@example.com";
            requestedCode = null;
            requestedUserInfoAccessToken = null;
        }

        String requestedCode() {
            return requestedCode;
        }

        String requestedUserInfoAccessToken() {
            return requestedUserInfoAccessToken;
        }

        @Override
        public KakaoTokenResponse requestAccessToken(String code) {
            requestedCode = code;
            return new KakaoTokenResponse(accessToken);
        }

        @Override
        public KakaoUserResponse requestUserInfo(String accessToken) {
            requestedUserInfoAccessToken = accessToken;
            return new KakaoUserResponse(new KakaoUserResponse.KakaoAccount(email));
        }
    }
}
