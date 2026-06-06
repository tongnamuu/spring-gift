package gift.product.controller;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AdminProductApiTest extends AbstractMysqlApiTest {
    private static final String TEST_CATEGORY_PREFIX = "admin-product-list-";
    private static final String TEST_PRODUCT_PREFIX = "admin-cat-";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

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
    void productListDisplaysUncategorizedWhenCategoryIsMissing() {
        Category category = categoryRepository.saveAndFlush(new Category(
            TEST_CATEGORY_PREFIX + "missing",
            "#123456",
            "https://example.com/admin-category.png",
            "admin product list category"
        ));
        Product product = productRepository.saveAndFlush(new Product(
            TEST_PRODUCT_PREFIX + "x",
            1_000,
            "https://example.com/admin-product.png",
            category.getId()
        ));
        categoryRepository.deleteById(category.getId());
        categoryRepository.flush();

        ResponseEntity<String> response = restTemplate.getForEntity("/admin/products", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(product.getName());
        assertThat(response.getBody()).contains("미분류 카테고리");
    }

    private void deleteTestData() {
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
    }
}
