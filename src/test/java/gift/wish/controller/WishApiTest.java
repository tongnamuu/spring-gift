package gift.wish.controller;

import gift.auth.JwtProvider;
import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlApiTest;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WishApiTest extends AbstractMysqlApiTest {
    private static final String TEST_EMAIL_PREFIX = "wish-api-";
    private static final String TEST_CATEGORY_PREFIX = "wish-api-category-";
    private static final String TEST_PRODUCT_PREFIX = "wap-";

    @Autowired
    private TestRestTemplate restTemplate;

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
    void getWishesReturnsUnauthorizedWithoutAuthorization() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/wishes", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getWishesReturnsOnlyAuthenticatedMembersWishes() {
        Member member = saveMember("list");
        Member other = saveMember("list-other");
        Product memberProduct = saveProduct("list");
        Product otherProduct = saveProduct("list-other");
        saveWish(member, memberProduct);
        saveWish(other, otherProduct);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.GET,
            authorizedEntity(member),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(memberProduct.getName());
        assertThat(response.getBody()).doesNotContain(otherProduct.getName());
    }

    @Test
    void addWishReturnsCreatedAndPersistsWish() {
        Member member = saveMember("add");
        Product product = saveProduct("add");

        ResponseEntity<WishResponse> response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            authorizedEntity(member, new WishRequest(product.getId())),
            WishResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().productId()).isEqualTo(product.getId());
        assertThat(countWishes(member.getId(), product.getId())).isEqualTo(1L);
    }

    @Test
    void addWishReturnsOkWhenMemberAlreadyWishedProduct() {
        Member member = saveMember("duplicate");
        Product product = saveProduct("duplicate");
        Wish existing = saveWish(member, product);

        ResponseEntity<WishResponse> response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            authorizedEntity(member, new WishRequest(product.getId())),
            WishResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(existing.getId());
        assertThat(countWishes(member.getId(), product.getId())).isEqualTo(1L);
    }

    @Test
    void addWishReturnsNotFoundWhenProductDoesNotExist() {
        Member member = saveMember("missing-product");

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            authorizedEntity(member, new WishRequest(Long.MAX_VALUE)),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addWishReturnsBadRequestWhenProductIdIsMissing() {
        Member member = saveMember("bad-request");

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/wishes",
            HttpMethod.POST,
            authorizedEntity(member, Map.of()),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void removeWishReturnsNoContentAndDeletesOwnedWish() {
        Member member = saveMember("delete");
        Product product = saveProduct("delete");
        Wish wish = saveWish(member, product);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/wishes/{id}",
            HttpMethod.DELETE,
            authorizedEntity(member),
            Void.class,
            wish.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(wishRepository.existsById(wish.getId())).isFalse();
    }

    @Test
    void removeWishReturnsForbiddenWhenWishBelongsToAnotherMember() {
        Member owner = saveMember("owner");
        Member other = saveMember("other");
        Product product = saveProduct("forbidden");
        Wish wish = saveWish(owner, product);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/wishes/{id}",
            HttpMethod.DELETE,
            authorizedEntity(other),
            Void.class,
            wish.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(wishRepository.existsById(wish.getId())).isTrue();
    }

    @Test
    void removeWishReturnsNotFoundWhenWishDoesNotExist() {
        Member member = saveMember("missing-wish");

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/wishes/{id}",
            HttpMethod.DELETE,
            authorizedEntity(member),
            Void.class,
            Long.MAX_VALUE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpEntity<Void> authorizedEntity(Member member) {
        return new HttpEntity<>(authorizationHeaders(member));
    }

    private HttpEntity<Object> authorizedEntity(Member member, Object body) {
        return new HttpEntity<>(body, authorizationHeaders(member));
    }

    private HttpHeaders authorizationHeaders(Member member) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtProvider.createToken(member.getEmail()));
        return headers;
    }

    private Member saveMember(String suffix) {
        return memberRepository.saveAndFlush(new Member(TEST_EMAIL_PREFIX + suffix + "@example.com", "password123"));
    }

    private Product saveProduct(String suffix) {
        Category category = categoryRepository.saveAndFlush(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#ABCDEF",
            "https://example.com/wish-category.png",
            "wish api test category"
        ));
        return productRepository.saveAndFlush(new Product(
            TEST_PRODUCT_PREFIX + Integer.toUnsignedString(suffix.hashCode(), 36),
            1000,
            "https://example.com/wish-product.png",
            category.getId()
        ));
    }

    private Wish saveWish(Member member, Product product) {
        return wishRepository.saveAndFlush(new Wish(member.getId(), product.getId()));
    }

    private long countWishes(Long memberId, Long productId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from wish where member_id = ? and product_id = ?",
            Long.class,
            memberId,
            productId
        );
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
