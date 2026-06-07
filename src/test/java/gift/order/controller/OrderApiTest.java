package gift.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.member.auth.JwtProvider;
import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.order.domain.Order;
import gift.order.domain.OrderRepository;
import gift.product.dto.OptionResponse;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.CreateOptionUseCase;
import gift.product.usecase.OptionCommand;
import gift.product.vo.OptionName;
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

class OrderApiTest extends AbstractMysqlApiTest {
    private static final String TEST_EMAIL_PREFIX = "order-api-";
    private static final String TEST_CATEGORY_PREFIX = "order-api-category-";
    private static final String TEST_PRODUCT_PREFIX = "ordapi-";
    private static final String PRODUCT_IMAGE_URL = "https://example.com/order-api-product.png";
    private static final int UNIT_PRICE = 1000;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CreateOptionUseCase createOptionUseCase;

    @Autowired
    private OrderRepository orderRepository;

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
    void getOrdersReturnsUnauthorizedWithoutAuthorization() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/orders", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createOrderReturnsUnauthorizedWithoutAuthorization() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/orders",
            new OrderRequest(Long.MAX_VALUE, 1, "unauthorized"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createOrderReturnsCreatedAndPersistsSnapshot() {
        Member member = saveMember("create", 10_000);
        Product product = saveProduct("create");
        OptionResponse option = saveOption(product, "basic-create", 5);

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            authorizedEntity(member, new OrderRequest(option.id(), 2, "api order")),
            OrderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().productId()).isEqualTo(product.getId());
        assertThat(response.getBody().optionId()).isEqualTo(option.id());
        assertThat(response.getBody().productName()).isEqualTo(product.getName());
        assertThat(response.getBody().optionName()).isEqualTo(option.name());
        assertThat(response.getBody().unitPrice()).isEqualTo(UNIT_PRICE);
        assertThat(response.getBody().totalPrice()).isEqualTo(UNIT_PRICE * 2);
        assertThat(response.getBody().productImageUrl()).isEqualTo(PRODUCT_IMAGE_URL);
        assertThat(response.getBody().quantity()).isEqualTo(2);
        assertThat(response.getBody().message()).isEqualTo("api order");
        assertThat(findOptionQuantity(option.id())).isEqualTo(3);
        assertThat(countOrdersByMember(member.getId())).isEqualTo(1L);
    }

    @Test
    void getOrdersReturnsOnlyAuthenticatedMembersOrderSnapshots() throws Exception {
        Member member = saveMember("list", 10_000);
        Member other = saveMember("list-other", 10_000);
        Product product = saveProduct("list");
        OptionResponse option = saveOption(product, "basic-list", 5);
        Order order = orderRepository.save(new Order(
            product.getId(),
            option.id(),
            member.getId(),
            product.getName(),
            option.name(),
            UNIT_PRICE,
            PRODUCT_IMAGE_URL,
            2,
            "member order"
        ));
        orderRepository.save(new Order(
            product.getId(),
            option.id(),
            other.getId(),
            product.getName(),
            option.name(),
            UNIT_PRICE,
            PRODUCT_IMAGE_URL,
            1,
            "other order"
        ));

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.GET,
            authorizedEntity(member),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("totalElements").asLong()).isEqualTo(1L);
        JsonNode first = root.path("content").get(0);
        assertThat(first.path("id").asLong()).isEqualTo(order.getId());
        assertThat(first.path("productId").asLong()).isEqualTo(product.getId());
        assertThat(first.path("optionId").asLong()).isEqualTo(option.id());
        assertThat(first.path("productName").asText()).isEqualTo(product.getName());
        assertThat(first.path("optionName").asText()).isEqualTo(option.name());
        assertThat(first.path("unitPrice").asInt()).isEqualTo(UNIT_PRICE);
        assertThat(first.path("totalPrice").asInt()).isEqualTo(UNIT_PRICE * 2);
        assertThat(first.path("message").asText()).isEqualTo("member order");
        assertThat(response.getBody()).doesNotContain("other order");
    }

    @Test
    void createOrderReturnsBadRequestWhenPointIsInsufficient() {
        Member member = saveMember("point-shortage", 500);
        Product product = saveProduct("point-shortage");
        OptionResponse option = saveOption(product, "basic-shortage", 5);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            authorizedEntity(member, new OrderRequest(option.id(), 1, "point shortage")),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("포인트가 부족합니다.");
        assertThat(countOrdersByMember(member.getId())).isZero();
        assertThat(findOptionQuantity(option.id())).isEqualTo(5);
    }

    @Test
    void createOrderReturnsNotFoundWhenOptionDoesNotExist() {
        Member member = saveMember("missing-option", 10_000);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            authorizedEntity(member, new OrderRequest(Long.MAX_VALUE, 1, "missing option")),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createOrderReturnsBadRequestWhenRequestIsInvalid() {
        Member member = saveMember("invalid", 10_000);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            authorizedEntity(member, Map.of("quantity", 1, "message", "missing option id")),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrderLeavesWishForRecurringPurchase() {
        Member member = saveMember("wish-retained", 10_000);
        Product product = saveProduct("wish-retained");
        OptionResponse option = saveOption(product, "basic-wish", 5);
        Wish wish = wishRepository.save(new Wish(member.getId(), product.getId()));

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            authorizedEntity(member, new OrderRequest(option.id(), 1, "recurring purchase")),
            OrderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(wishRepository.existsById(wish.getId())).isTrue();
        assertThat(countWishesByMemberAndProduct(member.getId(), product.getId())).isEqualTo(1L);
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

    private Member saveMember(String suffix, int point) {
        Member member = new Member(TEST_EMAIL_PREFIX + suffix + "@example.com", "password123");
        member.chargePoint(point);
        return memberRepository.save(member);
    }

    private Product saveProduct(String suffix) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#123456",
            "https://example.com/order-api-category.png",
            "order api test category"
        ));
        return productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + Integer.toUnsignedString(suffix.hashCode(), 36),
            UNIT_PRICE,
            PRODUCT_IMAGE_URL,
            category.getId()
        ));
    }

    private OptionResponse saveOption(Product product, String name, int quantity) {
        return createOptionUseCase.execute(product.getId(), new OptionCommand(new OptionName(name), quantity));
    }

    private int findOptionQuantity(Long optionId) {
        return jdbcTemplate.queryForObject(
            "select quantity from options where id = ?",
            Integer.class,
            optionId
        );
    }

    private long countOrdersByMember(Long memberId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from orders where member_id = ?",
            Long.class,
            memberId
        );
    }

    private long countWishesByMemberAndProduct(Long memberId, Long productId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from wish where member_id = ? and product_id = ?",
            Long.class,
            memberId,
            productId
        );
    }

    private void deleteTestData() {
        jdbcTemplate.update(
            "delete from orders where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
        );
        jdbcTemplate.update("delete from orders where product_name like ?", TEST_PRODUCT_PREFIX + "%");
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
