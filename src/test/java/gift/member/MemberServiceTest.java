package gift.member;

import gift.support.AbstractMysqlServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_EMAIL_PREFIX = "member-service-";
    private static final String DUPLICATE_EMAIL_MESSAGE = "Email is already registered.";

    @Autowired
    private CreateMemberUseCase createMemberUseCase;

    @Autowired
    private MemberRepository memberRepository;

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
    void createMemberPersistsMember() {
        String email = TEST_EMAIL_PREFIX + "create@example.com";

        Member member = createMemberUseCase.execute(email, "password123");

        assertThat(member.getId()).isNotNull();
        assertThat(member.getEmail()).isEqualTo(email);

        memberRepository.flush();

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select id, email, password, point from member where id = ?",
            member.getId()
        );
        assertThat(((Number) persisted.get("id")).longValue()).isEqualTo(member.getId());
        assertThat(persisted.get("email")).isEqualTo(email);
        assertThat(persisted.get("password")).isEqualTo("password123");
        assertThat(((Number) persisted.get("point")).intValue()).isZero();
    }

    @Test
    void createMemberRejectsDuplicateEmail() {
        String email = TEST_EMAIL_PREFIX + "duplicate@example.com";
        memberRepository.saveAndFlush(new Member(email, "password123"));

        assertThatThrownBy(() -> createMemberUseCase.execute(email, "another-password"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(DUPLICATE_EMAIL_MESSAGE);

        assertThat(countMembersByEmail(email)).isEqualTo(1L);
    }

    @Test
    void concurrentCreateMemberRejectsDuplicateEmailAndPersistsOnlyOneMember() throws Exception {
        String email = TEST_EMAIL_PREFIX + "concurrent@example.com";

        List<CreateMemberResult> results = createMemberConcurrently(email, 32);
        long successCount = results.stream()
            .filter(CreateMemberResult::succeeded)
            .count();
        List<Throwable> failures = results.stream()
            .filter(result -> !result.succeeded())
            .map(CreateMemberResult::failure)
            .toList();

        assertThat(successCount).isEqualTo(1L);
        assertThat(failures).hasSize(31);
        assertThat(failures)
            .allSatisfy(failure -> assertThat(failure)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DUPLICATE_EMAIL_MESSAGE));
        assertThat(countMembersByEmail(email)).isEqualTo(1L);
    }

    @Test
    void memberEmailHasDatabaseUniqueConstraint() {
        Integer uniqueIndexCount = jdbcTemplate.queryForObject(
            """
                select count(*)
                from information_schema.statistics
                where table_schema = database()
                    and table_name = 'member'
                    and column_name = 'email'
                    and non_unique = 0
                """,
            Integer.class
        );

        assertThat(uniqueIndexCount).isNotNull();
        assertThat(uniqueIndexCount).isPositive();
    }

    private List<CreateMemberResult> createMemberConcurrently(String email, int requestCount) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<CreateMemberResult>> futures = IntStream.range(0, requestCount)
                .mapToObj(ignored -> executorService.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out while waiting to start concurrent member creation.");
                    }
                    try {
                        createMemberUseCase.execute(email, "password123");
                        return CreateMemberResult.ok();
                    } catch (RuntimeException e) {
                        return CreateMemberResult.failure(e);
                    }
                }))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<CreateMemberResult> results = new ArrayList<>();
            for (Future<CreateMemberResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
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

    private void deleteTestMembers() {
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
    }

    private record CreateMemberResult(boolean succeeded, Throwable failure) {
        private static CreateMemberResult ok() {
            return new CreateMemberResult(true, null);
        }

        private static CreateMemberResult failure(Throwable failure) {
            return new CreateMemberResult(false, failure);
        }
    }
}
