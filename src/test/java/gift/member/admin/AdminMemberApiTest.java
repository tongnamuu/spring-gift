package gift.member.admin;

import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.order.domain.Order;
import gift.order.domain.OrderRepository;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

class AdminMemberApiTest extends AbstractMysqlApiTest {
    private static final String TEST_EMAIL_PREFIX = "admin-member-";
    private static final String TEST_CATEGORY_PREFIX = "admin-member-category-";
    private static final String TEST_PRODUCT_PREFIX = "am-";

    @Autowired
    private TestRestTemplate restTemplate;

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
    void memberListDisplaysMembers() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "list@example.com", "password123"));

        ResponseEntity<String> response = restTemplate.getForEntity("/admin/members", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(member.getEmail());
    }

    @Test
    void createMemberPersistsMember() {
        String email = TEST_EMAIL_PREFIX + "create@example.com";

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/admin/members",
            formEntity("email", email, "password", "password123"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberRepository.findByEmail(email)).isPresent();
    }

    @Test
    void createMemberShowsDuplicateEmailError() {
        String email = TEST_EMAIL_PREFIX + "duplicate@example.com";
        memberRepository.save(new Member(email, "password123"));

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/admin/members",
            formEntity("email", email, "password", "another-password"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Email is already registered.");
        assertThat(countMembersByEmail(email)).isEqualTo(1L);
    }

    @Test
    void editMemberPersistsChanges() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "before-edit@example.com", "password123"));
        String updatedEmail = TEST_EMAIL_PREFIX + "after-edit@example.com";

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/admin/members/" + member.getId() + "/edit",
            formEntity("email", updatedEmail, "password", "updated-password"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Member updated = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo(updatedEmail);
        assertThat(updated.getPassword()).isEqualTo("updated-password");
    }

    @Test
    void chargePointPersistsPoint() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "charge@example.com", "password123"));

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/admin/members/" + member.getId() + "/charge-point",
            formEntity("amount", "3000"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findPoint(member.getId())).isEqualTo(3000);
    }

    @Test
    void deleteMemberMarksMemberDeletedWithoutReferences() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "delete@example.com", "password123"));

        ResponseEntity<String> response = restTemplate.exchange(
            "/admin/members/{id}/delete",
            HttpMethod.POST,
            formEntity(),
            String.class,
            member.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberRepository.existsById(member.getId())).isTrue();
        assertThat(isDeleted(member.getId())).isTrue();
        assertThat(response.getBody()).doesNotContain(member.getEmail());
    }

    @Test
    void deleteMemberMarksMemberDeletedAndKeepsWishesWhenWishesReferenceMember() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "wish-delete@example.com", "password123"));
        Product product = saveProductWithOption("wish-delete");
        Wish wish = wishRepository.save(new Wish(member.getId(), product.getId()));

        ResponseEntity<String> response = restTemplate.exchange(
            "/admin/members/{id}/delete",
            HttpMethod.POST,
            formEntity(),
            String.class,
            member.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberRepository.existsById(member.getId())).isTrue();
        assertThat(isDeleted(member.getId())).isTrue();
        assertThat(wishRepository.existsById(wish.getId())).isTrue();
        assertThat(response.getBody()).doesNotContain(member.getEmail());
    }

    @Test
    void deleteMemberMarksMemberDeletedAndKeepsOrderHistory() {
        Member member = memberRepository.save(new Member(TEST_EMAIL_PREFIX + "order-delete@example.com", "password123"));
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

        ResponseEntity<String> response = restTemplate.exchange(
            "/admin/members/{id}/delete",
            HttpMethod.POST,
            formEntity(),
            String.class,
            member.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(memberRepository.existsById(member.getId())).isTrue();
        assertThat(isDeleted(member.getId())).isTrue();
        assertThat(orderRepository.existsById(order.getId())).isTrue();
        assertThat(response.getBody()).doesNotContain(member.getEmail());
    }

    private HttpEntity<MultiValueMap<String, String>> formEntity(String... values) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int index = 0; index < values.length; index += 2) {
            form.add(values[index], values[index + 1]);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(form, headers);
    }

    private long countMembersByEmail(String email) {
        return jdbcTemplate.queryForObject(
            "select count(*) from member where email = ?",
            Long.class,
            email
        );
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
            "https://example.com/admin-member-category-" + suffix + ".png",
            "admin member category " + suffix
        ));
        Product product = new Product(
            TEST_PRODUCT_PREFIX + suffix,
            1_000,
            "https://example.com/admin-member-product-" + suffix + ".png",
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
}
