package gift.member.auth;

import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.net.HttpURLConnection;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Import(KakaoAuthApiTest.KakaoAuthApiTestConfig.class)
@TestPropertySource(properties = {
    "kakao.login.client-id=test-client-id",
    "kakao.login.client-secret=test-client-secret",
    "kakao.login.redirect-uri=http://localhost:8080/api/auth/kakao/callback"
})
class KakaoAuthApiTest extends AbstractMysqlApiTest {
    private static final String TEST_EMAIL_PREFIX = "kakao-api-";

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

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
    void kakaoLoginRedirectsToAuthorizationUri() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI
            .create("http://localhost:" + port + "/api/auth/kakao/login")
            .toURL()
            .openConnection();
        connection.setInstanceFollowRedirects(false);

        assertThat(connection.getResponseCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(connection.getHeaderField("Location"))
            .startsWith("https://kauth.kakao.com/oauth/authorize")
            .contains("client_id=test-client-id")
            .contains("scope=account_email,talk_message");
    }

    @Test
    void kakaoCallbackCreatesMemberAndReturnsToken() {
        String email = TEST_EMAIL_PREFIX + "callback@example.com";
        kakaoLoginClient.prepare("kakao-api-access-token", email);

        ResponseEntity<TokenResponse> response = restTemplate.getForEntity(
            "/api/auth/kakao/callback?code=authorization-code",
            TokenResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(jwtProvider.getEmail(response.getBody().token())).isEqualTo(email);
        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-api-access-token");
    }

    private void deleteTestMembers() {
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
    }

    @TestConfiguration
    static class KakaoAuthApiTestConfig {
        @Bean
        @Primary
        FakeKakaoLoginClient fakeKakaoLoginClient() {
            return new FakeKakaoLoginClient();
        }
    }

    static class FakeKakaoLoginClient implements KakaoLoginClient {
        private String accessToken = "kakao-api-access-token";
        private String email = TEST_EMAIL_PREFIX + "default@example.com";

        void prepare(String accessToken, String email) {
            this.accessToken = accessToken;
            this.email = email;
        }

        void reset() {
            accessToken = "kakao-api-access-token";
            email = TEST_EMAIL_PREFIX + "default@example.com";
        }

        @Override
        public KakaoTokenResponse requestAccessToken(String code) {
            return new KakaoTokenResponse(accessToken);
        }

        @Override
        public KakaoUserResponse requestUserInfo(String accessToken) {
            return new KakaoUserResponse(new KakaoUserResponse.KakaoAccount(email));
        }
    }
}
