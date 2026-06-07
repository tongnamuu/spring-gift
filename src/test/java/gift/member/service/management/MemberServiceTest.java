package gift.member.service.management;

import gift.member.auth.JwtProvider;
import gift.member.auth.TokenResponse;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.order.domain.Order;
import gift.order.domain.OrderRepository;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.member.usecase.management.ChargeMemberPointUseCase;
import gift.member.usecase.management.CreateMemberUseCase;
import gift.member.usecase.management.DeleteMemberUseCase;
import gift.member.usecase.management.GetMemberUseCase;
import gift.member.usecase.management.GetMembersUseCase;
import gift.member.usecase.auth.LoginMemberUseCase;
import gift.member.dto.MemberCredentialsCommand;
import gift.member.usecase.management.UpdateMemberUseCase;
import gift.member.vo.Password;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
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
    private static final String TEST_CATEGORY_PREFIX = "member-service-category-";
    private static final String TEST_PRODUCT_PREFIX = "ms-";
    private static final String DUPLICATE_EMAIL_MESSAGE = "Email is already registered.";
    private static final String MEMBER_NOT_FOUND_MESSAGE = "회원이 존재하지 않습니다.";

    @Autowired
    private CreateMemberUseCase createMemberUseCase;

    @Autowired
    private GetMembersUseCase getMembersUseCase;

    @Autowired
    private GetMemberUseCase getMemberUseCase;

    @Autowired
    private UpdateMemberUseCase updateMemberUseCase;

    @Autowired
    private DeleteMemberUseCase deleteMemberUseCase;

    @Autowired
    private LoginMemberUseCase loginMemberUseCase;

    @Autowired
    private ChargeMemberPointUseCase chargeMemberPointUseCase;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private OrderRepository orderRepository;

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

        Member member = createMemberUseCase.execute(email, password("password123"));

        assertThat(member.getId()).isNotNull();
        assertThat(member.getEmail()).isEqualTo(email);

        memberRepository.flush();

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select id, email, password, point from member where id = ?",
            member.getId()
        );
        assertThat(((Number) persisted.get("id")).longValue()).isEqualTo(member.getId());
        assertThat(persisted.get("email")).isEqualTo(email);
        String persistedPassword = (String) persisted.get("password");
        assertThat(persistedPassword).isNotEqualTo("password123");
        assertThat(password("password123").matches(persistedPassword)).isTrue();
        assertThat(((Number) persisted.get("point")).intValue()).isZero();
    }

    @Test
    void createMemberRejectsDuplicateEmail() {
        String email = TEST_EMAIL_PREFIX + "duplicate@example.com";
        memberRepository.save(new Member(email, Password.encode("password123")));

        assertThatThrownBy(() -> createMemberUseCase.execute(email, password("another-password")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(DUPLICATE_EMAIL_MESSAGE);

        assertThat(countMembersByEmail(email)).isEqualTo(1L);
    }

    @Test
    void createMemberRejectsDeletedMemberEmail() {
        String email = TEST_EMAIL_PREFIX + "deleted-register@example.com";
        Member member = memberRepository.save(new Member(email, Password.encode("password123")));
        member.markDeleted();
        memberRepository.save(member);

        assertThatThrownBy(() -> createMemberUseCase.execute(email, password("another-password")))
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

    @Test
    void loginMemberReturnsTokenForRegisteredMember() {
        String email = TEST_EMAIL_PREFIX + "login@example.com";
        saveMemberWithPassword(email, "password123");

        TokenResponse response = loginMemberUseCase.execute(
            new MemberCredentialsCommand(email, password("password123"))
        );

        assertThat(response.token()).isNotBlank();
        assertThat(jwtProvider.getEmail(response.token())).isEqualTo(email);
    }

    @Test
    void loginMemberRejectsWrongPassword() {
        String email = TEST_EMAIL_PREFIX + "wrong-password@example.com";
        saveMemberWithPassword(email, "password123");

        assertThatThrownBy(() -> loginMemberUseCase.execute(
            new MemberCredentialsCommand(email, password("wrong-password"))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid email or password.");
    }

    @Test
    void loginMemberRejectsMissingMember() {
        assertThatThrownBy(() -> loginMemberUseCase.execute(
            new MemberCredentialsCommand(TEST_EMAIL_PREFIX + "missing@example.com", password("password123"))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid email or password.");
    }

    @Test
    void loginMemberRejectsDeletedMember() {
        String email = TEST_EMAIL_PREFIX + "deleted-login@example.com";
        Member member = saveMemberWithPassword(email, "password123");
        member.markDeleted();
        memberRepository.save(member);

        assertThatThrownBy(() -> loginMemberUseCase.execute(
            new MemberCredentialsCommand(email, password("password123"))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid email or password.");
    }

    @Test
    void chargeMemberPointUpdatesPersistedPoint() {
        String email = TEST_EMAIL_PREFIX + "charge@example.com";
        Member member = memberRepository.save(new Member(email, Password.encode("password123")));

        Member charged = chargeMemberPointUseCase.execute(member.getId(), 3000);

        assertThat(charged.getPoint()).isEqualTo(3000);
        assertThat(findPoint(member.getId())).isEqualTo(3000);
    }

    @Test
    void chargeMemberPointRejectsMissingMember() {
        assertThatThrownBy(() -> chargeMemberPointUseCase.execute(Long.MAX_VALUE, 3000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(MEMBER_NOT_FOUND_MESSAGE);
    }

    @Test
    void chargeMemberPointRejectsNonPositiveAmount() {
        String email = TEST_EMAIL_PREFIX + "invalid-charge@example.com";
        Member member = memberRepository.save(new Member(email, Password.encode("password123")));

        assertThatThrownBy(() -> chargeMemberPointUseCase.execute(member.getId(), 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount must be greater than zero.");
    }

    @Test
    void getMembersReturnsMembers() {
        Member first = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "list-1@example.com", Password.encode("password123")));
        Member second = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "list-2@example.com", Password.encode("password123")));

        List<String> emails = getMembersUseCase.execute().stream()
            .map(Member::getEmail)
            .toList();

        assertThat(emails).contains(first.getEmail(), second.getEmail());
    }

    @Test
    void getMemberReturnsMemberById() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "get@example.com", Password.encode("password123")));

        assertThat(getMemberUseCase.execute(member.getId()))
            .hasValueSatisfying(found -> assertThat(found.getEmail()).isEqualTo(member.getEmail()));
    }

    @Test
    void getMemberReturnsEmptyWhenMemberDoesNotExist() {
        assertThat(getMemberUseCase.execute(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void getMembersDoesNotReturnDeletedMembers() {
        Member active = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "active-list@example.com", Password.encode("password123")));
        Member deleted = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "deleted-list@example.com", Password.encode("password123")));
        deleted.markDeleted();
        memberRepository.save(deleted);

        List<Long> ids = getMembersUseCase.execute().stream()
            .map(Member::getId)
            .toList();

        assertThat(ids).contains(active.getId());
        assertThat(ids).doesNotContain(deleted.getId());
    }

    @Test
    void getMemberReturnsEmptyWhenMemberIsDeleted() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "deleted-get@example.com", Password.encode("password123")));
        member.markDeleted();
        memberRepository.save(member);

        assertThat(getMemberUseCase.execute(member.getId())).isEmpty();
    }

    @Test
    void updateMemberUpdatesEmailAndPassword() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "before-update@example.com", Password.encode("password123")));
        String updatedEmail = TEST_EMAIL_PREFIX + "after-update@example.com";

        Member updated = updateMemberUseCase.execute(member.getId(), updatedEmail, password("updated-password"));

        assertThat(updated.getEmail()).isEqualTo(updatedEmail);
        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select email, password from member where id = ?",
            member.getId()
        );
        assertThat(persisted.get("email")).isEqualTo(updatedEmail);
        String persistedPassword = (String) persisted.get("password");
        assertThat(persistedPassword).isNotEqualTo("updated-password");
        assertThat(password("updated-password").matches(persistedPassword)).isTrue();
    }

    @Test
    void updateMemberRejectsKakaoAccountPasswordChange() {
        String email = TEST_EMAIL_PREFIX + "kakao-update@example.com";
        Member member = new Member(email);
        member.updateKakaoAccessToken("kakao-access-token");
        Member saved = memberRepository.save(member);

        assertThatThrownBy(() -> updateMemberUseCase.execute(
            saved.getId(),
            TEST_EMAIL_PREFIX + "kakao-update-changed@example.com",
            password("updated-password")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("카카오 계정은 비밀번호를 변경할 수 없습니다.");

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select email, password, kakao_access_token from member where id = ?",
            saved.getId()
        );
        assertThat(persisted.get("email")).isEqualTo(email);
        assertThat(persisted.get("password")).isNull();
        assertThat(persisted.get("kakao_access_token")).isEqualTo("kakao-access-token");
    }

    @Test
    void updateMemberRejectsMissingMember() {
        assertThatThrownBy(() -> updateMemberUseCase.execute(
            Long.MAX_VALUE,
            TEST_EMAIL_PREFIX + "missing-update@example.com",
            password("password123")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(MEMBER_NOT_FOUND_MESSAGE);
    }

    @Test
    void updateMemberRejectsDeletedMember() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "deleted-update@example.com", Password.encode("password123")));
        member.markDeleted();
        memberRepository.save(member);

        assertThatThrownBy(() -> updateMemberUseCase.execute(
            member.getId(),
            TEST_EMAIL_PREFIX + "after-deleted-update@example.com",
            password("updated-password")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(MEMBER_NOT_FOUND_MESSAGE);
    }

    @Test
    void chargeMemberPointRejectsDeletedMember() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "deleted-charge@example.com", Password.encode("password123")));
        member.markDeleted();
        memberRepository.save(member);

        assertThatThrownBy(() -> chargeMemberPointUseCase.execute(member.getId(), 3000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(MEMBER_NOT_FOUND_MESSAGE);
    }

    @Test
    void deleteMemberMarksMemberDeletedWithoutReferences() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "delete@example.com", Password.encode("password123")));

        deleteMemberUseCase.execute(member.getId());

        assertThat(memberRepository.existsById(member.getId())).isTrue();
        assertThat(isDeleted(member.getId())).isTrue();
        assertThat(getMemberUseCase.execute(member.getId())).isEmpty();
    }

    @Test
    void deleteMemberMarksMemberDeletedAndKeepsWishesWhenWishesReferenceMember() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "wish-delete@example.com", Password.encode("password123")));
        Product product = saveProductWithOption("wish-delete");
        Wish wish = wishRepository.save(new Wish(member.getId(), product.getId()));

        deleteMemberUseCase.execute(member.getId());

        assertThat(memberRepository.existsById(member.getId())).isTrue();
        assertThat(isDeleted(member.getId())).isTrue();
        assertThat(wishRepository.existsById(wish.getId())).isTrue();
    }

    @Test
    void deleteMemberMarksMemberDeletedAndKeepsOrderHistory() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "order-delete@example.com", Password.encode("password123")));
        Product product = saveProductWithOption("order-delete");
        Option option = product.getOptions().getFirst();
        Order order = orderRepository.save(new Order(
            product.getId(),
            option.getId(),
            member.getId(),
            product.getName(),
            option.getName(),
            product.getPrice(),
            product.getImageUrl(),
            1,
            "message"
        ));

        deleteMemberUseCase.execute(member.getId());

        assertThat(memberRepository.existsById(member.getId())).isTrue();
        assertThat(isDeleted(member.getId())).isTrue();
        assertThat(orderRepository.existsById(order.getId())).isTrue();
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
                        createMemberUseCase.execute(email, password("password123"));
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

    private Member saveMemberWithPassword(String email, String password) {
        return memberRepository.save(new Member(email, Password.encode(password)));
    }

    private Password password(String rawValue) {
        return Password.encode(rawValue);
    }

    private int findPoint(Long memberId) {
        return jdbcTemplate.queryForObject(
            "select point from member where id = ?",
            Integer.class,
            memberId
        );
    }

    private boolean isDeleted(Long memberId) {
        return jdbcTemplate.queryForObject(
            "select deleted from member where id = ?",
            Boolean.class,
            memberId
        );
    }

    private Product saveProductWithOption(String suffix) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#123456",
            "https://example.com/member-category-" + suffix + ".png",
            "member service category " + suffix
        ));
        Product product = new Product(
            TEST_PRODUCT_PREFIX + suffix,
            1_000,
            "https://example.com/member-product-" + suffix + ".png",
            category.getId()
        );
        product.addOption("option-" + suffix, 10);
        Product saved = productRepository.save(product);
        productRepository.flush();
        return saved;
    }

    private void deleteTestMembers() {
        jdbcTemplate.update(
            "delete from orders where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
        );
        jdbcTemplate.update(
            "delete from wish where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
        );
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
        jdbcTemplate.update(
            "delete from options where product_id in (select id from product where name like ?)",
            TEST_PRODUCT_PREFIX + "%"
        );
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
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
