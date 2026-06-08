package gift.wish.service;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.member.vo.Password;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlServiceTest;
import gift.wish.controller.WishResponse;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import gift.wish.query.GetWishesService;
import gift.wish.usecase.AddWishResult;
import gift.wish.dto.WishCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static gift.product.support.ProductFixtures.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WishServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_EMAIL_PREFIX = "wish-service-";
    private static final String TEST_CATEGORY_PREFIX = "wish-service-category-";
    private static final String TEST_PRODUCT_PREFIX = "wsp-";

    @Autowired
    private GetWishesService getWishesService;

    @Autowired
    private AddWishService addWishService;

    @Autowired
    private RemoveWishService removeWishService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        deleteTestData();
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    @Test
    void addWishPersistsWishForMemberAndProduct() {
        Member member = saveMember("add");
        Product product = saveProduct("add");

        AddWishResult result = addWishService.execute(member.getId(), new WishCommand(product.getId()));

        assertThat(result.created()).isTrue();
        assertThat(result.response().id()).isNotNull();
        assertThat(result.response().productId()).isEqualTo(product.getId());
        assertThat(result.response().name()).isEqualTo(product.getName());

        wishRepository.flush();

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select id, member_id, product_id from wish where id = ?",
            result.response().id()
        );
        assertThat(((Number) persisted.get("id")).longValue()).isEqualTo(result.response().id());
        assertThat(((Number) persisted.get("member_id")).longValue()).isEqualTo(member.getId());
        assertThat(((Number) persisted.get("product_id")).longValue()).isEqualTo(product.getId());
    }

    @Test
    void addWishThrowsWhenMemberAlreadyWishedProduct() {
        Member member = saveMember("duplicate");
        Product product = saveProduct("duplicate");
        saveWish(member, product);

        assertThatThrownBy(() -> addWishService.execute(member.getId(), new WishCommand(product.getId())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 위시한 상품입니다.");

        assertThat(countWishes(member.getId(), product.getId())).isEqualTo(1L);
    }

    @Test
    void concurrentAddWishPersistsOnlyOneWishForMemberAndProduct() throws Exception {
        Member member = saveMember("concurrent");
        Product product = saveProduct("concurrent");

        List<ConcurrentWishAttempt> attempts = addWishConcurrently(member.getId(), product.getId(), 16);

        assertThat(attempts)
            .filteredOn(ConcurrentWishAttempt::succeeded)
            .hasSize(1);
        assertThat(attempts)
            .filteredOn(attempt -> !attempt.succeeded())
            .hasSize(15)
            .allSatisfy(attempt -> assertThat(attempt.failure())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 위시한 상품입니다."));
        assertThat(countWishes(member.getId(), product.getId())).isEqualTo(1L);
    }

    @Test
    void wishMemberAndProductHasDatabaseUniqueConstraint() {
        Integer uniqueIndexColumnCount = jdbcTemplate.queryForObject(
            """
                select count(*)
                from information_schema.statistics
                where table_schema = database()
                    and table_name = 'wish'
                    and index_name = 'uk_wish_member_product'
                    and non_unique = 0
                """,
            Integer.class
        );

        assertThat(uniqueIndexColumnCount).isNotNull();
        assertThat(uniqueIndexColumnCount).isEqualTo(2);
    }

    @Test
    void addWishThrowsWhenProductDoesNotExist() {
        Member member = saveMember("missing-product");

        assertThatThrownBy(() -> addWishService.execute(member.getId(), new WishCommand(Long.MAX_VALUE)))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getWishesReturnsOnlyRequestedMembersWishes() {
        Member owner = saveMember("owner");
        Member other = saveMember("other");
        Product ownerProduct = saveProduct("owner");
        Product otherProduct = saveProduct("other");
        saveWish(owner, ownerProduct);
        saveWish(other, otherProduct);

        Page<WishResponse> response = getWishesService.execute(owner.getId(), PageRequest.of(0, 20));

        assertThat(response.getContent())
            .extracting(WishResponse::productId)
            .contains(ownerProduct.getId())
            .doesNotContain(otherProduct.getId());
    }

    @Test
    void getWishesReturnsJoinedProductPayload() {
        Member owner = saveMember("batch-owner");
        Product firstProduct = saveProduct("batch-first");
        Product secondProduct = saveProduct("batch-second");
        saveWish(owner, firstProduct);
        saveWish(owner, secondProduct);

        Page<WishResponse> response = getWishesService.execute(owner.getId(), PageRequest.of(0, 20));

        assertThat(response.getContent())
            .extracting(WishResponse::productId)
            .contains(firstProduct.getId(), secondProduct.getId());
        assertThat(response.getContent())
            .extracting(WishResponse::name)
            .contains(firstProduct.getName(), secondProduct.getName());
    }

    @Test
    void getWishesDoesNotExposeWishWhenProductIsMissing() {
        Member owner = saveMember("missing-joined-product");
        Product visibleProduct = saveProduct("visible-product");
        saveWish(owner, visibleProduct);
        Long missingProductId = Long.MAX_VALUE;
        saveWishWithMissingProduct(owner.getId(), missingProductId);

        Page<WishResponse> response = getWishesService.execute(
            owner.getId(),
            PageRequest.of(0, 20, Sort.by("productId"))
        );

        assertThat(response.getContent())
            .extracting(WishResponse::productId)
            .containsExactly(visibleProduct.getId())
            .doesNotContain(missingProductId);
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void removeWishDeletesOwnedWish() {
        Member member = saveMember("delete");
        Product product = saveProduct("delete");
        Wish wish = saveWish(member, product);

        removeWishService.execute(member.getId(), wish.getId());

        assertThat(wishRepository.existsById(wish.getId())).isFalse();
    }

    @Test
    void removeWishRejectsOtherMembersWish() {
        Member owner = saveMember("delete-owner");
        Member other = saveMember("delete-other");
        Product product = saveProduct("delete-other");
        Wish wish = saveWish(owner, product);

        assertThatThrownBy(() -> removeWishService.execute(other.getId(), wish.getId()))
            .isInstanceOf(WishAccessDeniedException.class);
        assertThat(wishRepository.existsById(wish.getId())).isTrue();
    }

    @Test
    void removeWishThrowsWhenWishDoesNotExist() {
        Member member = saveMember("missing-wish");

        assertThatThrownBy(() -> removeWishService.execute(member.getId(), Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);
    }

    private Member saveMember(String suffix) {
        return memberRepository.save(new Member(TEST_EMAIL_PREFIX + suffix + "@example.com", Password.encode("password123")));
    }

    private Product saveProduct(String suffix) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#ABCDEF",
            "https://example.com/wish-category.png",
            "wish service test category"
        ));
        return productRepository.save(product(
            TEST_PRODUCT_PREFIX + Integer.toUnsignedString(suffix.hashCode(), 36),
            1000,
            "https://example.com/wish-product.png",
            category.getId()
        ));
    }

    private Wish saveWish(Member member, Product product) {
        return wishRepository.save(new Wish(member.getId(), product.getId()));
    }

    private void saveWishWithMissingProduct(Long memberId, Long productId) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("set foreign_key_checks = 0");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "insert into wish (member_id, product_id) values (?, ?)"
            )) {
                statement.setLong(1, memberId);
                statement.setLong(2, productId);
                statement.executeUpdate();
            } finally {
                try (var statement = connection.createStatement()) {
                    statement.execute("set foreign_key_checks = 1");
                }
            }
            return null;
        });
    }

    private long countWishes(Long memberId, Long productId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from wish where member_id = ? and product_id = ?",
            Long.class,
            memberId,
            productId
        );
    }

    private List<ConcurrentWishAttempt> addWishConcurrently(Long memberId, Long productId, int requestCount) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ConcurrentWishAttempt>> futures = new ArrayList<>();

        try {
            IntStream.range(0, requestCount)
                .forEach(ignored -> futures.add(executorService.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out while waiting to start concurrent wish requests.");
                    }
                    try {
                        return ConcurrentWishAttempt.success(addWishService.execute(memberId, new WishCommand(productId)));
                    } catch (RuntimeException e) {
                        return ConcurrentWishAttempt.failure(e);
                    }
                })));

            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while preparing concurrent wish requests.");
            }
            start.countDown();

            List<ConcurrentWishAttempt> results = new ArrayList<>();
            for (Future<ConcurrentWishAttempt> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private record ConcurrentWishAttempt(AddWishResult result, RuntimeException failure) {
        private static ConcurrentWishAttempt success(AddWishResult result) {
            return new ConcurrentWishAttempt(result, null);
        }

        private static ConcurrentWishAttempt failure(RuntimeException failure) {
            return new ConcurrentWishAttempt(null, failure);
        }

        private boolean succeeded() {
            return result != null;
        }
    }

    private void deleteTestData() {
        jdbcTemplate.update(
            "delete from wish where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
        );
        jdbcTemplate.update(
            "delete from wish where product_id in (select id from product where name like ?)",
            TEST_PRODUCT_PREFIX + "%"
        );
        jdbcTemplate.update(
            "delete from options where product_id in (select id from product where name like ?)",
            TEST_PRODUCT_PREFIX + "%"
        );
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
    }
}
