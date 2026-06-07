package gift.order;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.OptionRepository;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_EMAIL_PREFIX = "order-service-";
    private static final String TEST_CATEGORY_PREFIX = "os-cat-";
    private static final String TEST_PRODUCT_PREFIX = "os-p-";
    private static final String ORIGINAL_PRODUCT_NAME = TEST_PRODUCT_PREFIX + "2025";
    private static final String CHANGED_PRODUCT_NAME = TEST_PRODUCT_PREFIX + "2026";
    private static final String ORIGINAL_OPTION_NAME = "2025년 재배 햅쌀";
    private static final int ORIGINAL_UNIT_PRICE = 30000;
    private static final int CHANGED_UNIT_PRICE = 35000;
    private static final String ORIGINAL_IMAGE_URL = "https://example.com/rice-2025.png";
    private static final String CHANGED_IMAGE_URL = "https://example.com/rice-2026.png";

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

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
    void orderListUsesSnapshotCapturedAtOrderCreation() {
        Member member = saveMember();
        Category category = saveCategory();
        Product product = saveProduct(category);
        Option option = optionRepository.save(new Option(product, ORIGINAL_OPTION_NAME, 10));

        OrderResponse created = createOrderUseCase.execute(
            member.getId(),
            new OrderRequest(option.getId(), 2, "2025년 햅쌀 주문")
        );
        updateProductAfterOrder(product.getId(), category.getId());

        OrderResponse listed = orderRepository.findByMemberId(member.getId(), Pageable.unpaged())
            .map(OrderResponse::from)
            .getContent()
            .get(0);

        assertSnapshot(created);
        assertSnapshot(listed);
    }

    @Test
    void createOrderFindsProductBySelectedOptionWhenProductHasMultipleOptions() {
        Member member = saveMember();
        Category category = saveCategory();
        Product product = saveProduct(category);
        Option firstOption = optionRepository.save(new Option(product, "2024년 재배 햅쌀", 10));
        Option secondOption = optionRepository.save(new Option(product, ORIGINAL_OPTION_NAME, 20));

        OrderResponse response = createOrderUseCase.execute(
            member.getId(),
            new OrderRequest(secondOption.getId(), 3, "두 번째 옵션 주문")
        );

        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.optionId()).isEqualTo(secondOption.getId());
        assertThat(response.optionName()).isEqualTo(ORIGINAL_OPTION_NAME);
        assertThat(findOptionQuantity(firstOption.getId())).isEqualTo(10);
        assertThat(findOptionQuantity(secondOption.getId())).isEqualTo(17);
    }

    private void assertSnapshot(OrderResponse response) {
        assertThat(response.productName()).isEqualTo(ORIGINAL_PRODUCT_NAME);
        assertThat(response.optionName()).isEqualTo(ORIGINAL_OPTION_NAME);
        assertThat(response.unitPrice()).isEqualTo(ORIGINAL_UNIT_PRICE);
        assertThat(response.totalPrice()).isEqualTo(ORIGINAL_UNIT_PRICE * 2);
        assertThat(response.productImageUrl()).isEqualTo(ORIGINAL_IMAGE_URL);
        assertThat(response.quantity()).isEqualTo(2);
    }

    private Member saveMember() {
        Member member = new Member(TEST_EMAIL_PREFIX + "snapshot@example.com", "password123");
        member.chargePoint(100000);
        return memberRepository.save(member);
    }

    private Category saveCategory() {
        return categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + "snapshot",
            "#123456",
            "https://example.com/order-category.png",
            "order service test category"
        ));
    }

    private Product saveProduct(Category category) {
        return productRepository.save(new Product(
            ORIGINAL_PRODUCT_NAME,
            ORIGINAL_UNIT_PRICE,
            ORIGINAL_IMAGE_URL,
            category.getId()
        ));
    }

    private void updateProductAfterOrder(Long productId, Long categoryId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        product.update(CHANGED_PRODUCT_NAME, CHANGED_UNIT_PRICE, CHANGED_IMAGE_URL, categoryId);
        productRepository.save(product);
    }

    private int findOptionQuantity(Long optionId) {
        return jdbcTemplate.queryForObject(
            "select quantity from options where id = ?",
            Integer.class,
            optionId
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
