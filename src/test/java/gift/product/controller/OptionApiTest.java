package gift.product.controller;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.dto.OptionRequest;
import gift.product.dto.OptionResponse;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.dto.OptionCommand;
import gift.product.usecase.CreateOptionUseCase;
import gift.product.vo.OptionName;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class OptionApiTest extends AbstractMysqlApiTest {
    private static final String TEST_CATEGORY_PREFIX = "option-api-category-";
    private static final String TEST_PRODUCT_PREFIX = "oap-";
    private static final String TEST_OPTION_PREFIX = "option-api-option-";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CreateOptionUseCase createOptionUseCase;

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
    void deleteLastOptionReturnsBadRequestWithRuleMessage() {
        Product product = saveProduct();
        OptionResponse option = createOptionUseCase.execute(
            product.getId(),
            new OptionCommand(new OptionName(TEST_OPTION_PREFIX + "last"), 10)
        );

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/products/{productId}/options/{optionId}",
            HttpMethod.DELETE,
            null,
            String.class,
            product.getId(),
            option.id()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        assertThat(optionExists(option.id())).isTrue();
    }

    private Product saveProduct() {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + "last",
            "#123456",
            "https://example.com/option-api-category.png",
            "option api test category"
        ));
        return productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + "last",
            10_000,
            "https://example.com/option-api-product.png",
            category.getId()
        ));
    }

    private void deleteTestData() {
        jdbcTemplate.update(
            "delete from orders where option_id in (select id from options where product_id in (select id from product where name like ?))",
            TEST_PRODUCT_PREFIX + "%"
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
    }

    private boolean optionExists(Long optionId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from options where id = ?",
            Long.class,
            optionId
        ) > 0;
    }
}
