package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MemberApiTest extends AbstractMysqlApiTest {
    private static final String TEST_EMAIL_PREFIX = "member-api-";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        deleteTestMembers();
    }

    @AfterEach
    void tearDown() {
        deleteTestMembers();
    }

    @Test
    void registerReturnsCreatedTokenAndPersistsMember() {
        MemberRequest request = new MemberRequest(TEST_EMAIL_PREFIX + "register@example.com", "password123");

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            "/api/members/register",
            request,
            TokenResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(jwtProvider.getEmail(response.getBody().token())).isEqualTo(request.email());

        Member persisted = memberRepository.findByEmail(request.email()).orElseThrow();
        assertThat(persisted.getEmail()).isEqualTo(request.email());
    }

    @Test
    void registerReturnsBadRequestWhenEmailAlreadyExists() {
        MemberRequest request = new MemberRequest(TEST_EMAIL_PREFIX + "duplicate@example.com", "password123");
        memberRepository.saveAndFlush(new Member(request.email(), request.password()));

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/members/register",
            request,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Email is already registered.");
    }

    @Test
    void concurrentDuplicateRegisterReturnsServerErrorForCurrentImplementation() throws Exception {
        MemberRequest request = new MemberRequest(TEST_EMAIL_PREFIX + "concurrent-duplicate@example.com", "password123");

        List<ResponseEntity<String>> responses = postRegisterConcurrently(request, 32);
        List<HttpStatusCode> statusCodes = responses.stream()
            .map(ResponseEntity::getStatusCode)
            .toList();

        assertThat(statusCodes).contains(HttpStatus.CREATED, HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(findResponseBodyByStatus(responses, HttpStatus.INTERNAL_SERVER_ERROR))
            .contains("\"error\":\"Internal Server Error\"");
        assertThat(countMembersByEmail(request.email())).isEqualTo(1L);
    }

    @Test
    void loginReturnsOkTokenForRegisteredMember() {
        MemberRequest request = new MemberRequest(TEST_EMAIL_PREFIX + "login@example.com", "password123");
        memberRepository.saveAndFlush(new Member(request.email(), request.password()));

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            "/api/members/login",
            request,
            TokenResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(jwtProvider.getEmail(response.getBody().token())).isEqualTo(request.email());
    }

    @Test
    void loginReturnsBadRequestWhenPasswordDoesNotMatch() {
        String email = TEST_EMAIL_PREFIX + "wrong-password@example.com";
        memberRepository.saveAndFlush(new Member(email, "password123"));

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/members/login",
            new MemberRequest(email, "wrong-password"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid email or password.");
    }

    @Test
    void loginReturnsBadRequestWhenMemberDoesNotExist() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/members/login",
            new MemberRequest(TEST_EMAIL_PREFIX + "missing@example.com", "password123"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid email or password.");
    }

    @Test
    void registerReturnsBadRequestWhenEmailIsInvalid() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/members/register",
            new MemberRequest("not-an-email", "password123"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void deleteTestMembers() {
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
    }

    private List<ResponseEntity<String>> postRegisterConcurrently(MemberRequest request, int requestCount) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<ResponseEntity<String>>> futures = IntStream.range(0, requestCount)
                .mapToObj(ignored -> executorService.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out while waiting to start concurrent register requests.");
                    }
                    return restTemplate.postForEntity("/api/members/register", request, String.class);
                }))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<ResponseEntity<String>> responses = new ArrayList<>();
            for (Future<ResponseEntity<String>> future : futures) {
                responses.add(future.get(10, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            executorService.shutdownNow();
        }
    }

    private long countMembersByEmail(String email) {
        return jdbcTemplate.queryForObject(
            "select count(*) from member where email = ?",
            Long.class,
            email
        );
    }

    private String findResponseBodyByStatus(List<ResponseEntity<String>> responses, HttpStatus status) {
        return responses.stream()
            .filter(response -> response.getStatusCode().equals(status))
            .map(ResponseEntity::getBody)
            .findFirst()
            .orElseThrow();
    }
}
