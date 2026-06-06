package gift.category;

import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
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

class CategoryApiTest extends AbstractMysqlApiTest {
    private static final String TEST_CATEGORY_PREFIX = "api-test-category-";
    private static final String TEST_PRODUCT_PREFIX = "cat-api-";

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
    void deleteCategoryReturnsNoContentWhenNoProductReferencesCategory() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "delete-ok");

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/categories/{id}",
            HttpMethod.DELETE,
            null,
            Void.class,
            category.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(categoryRepository.existsById(category.getId())).isFalse();
    }

    @Test
    void deleteCategoryReturnsBadRequestWhenProductReferencesCategory() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "delete-referenced");
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "ref", category.getId());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/categories/{id}",
            HttpMethod.DELETE,
            null,
            String.class,
            category.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("상품이 있는 카테고리는 삭제할 수 없습니다.");
        assertThat(categoryRepository.existsById(category.getId())).isTrue();
        assertThat(productRepository.existsById(product.getId())).isTrue();
    }

    private Category saveCategory(String name) {
        return categoryRepository.saveAndFlush(new Category(
            name,
            "#ABCDEF",
            "https://example.com/category-api-test.png",
            "category api test"
        ));
    }

    private Product saveProduct(String name, Long categoryId) {
        return productRepository.saveAndFlush(new Product(
            name,
            1_000,
            "https://example.com/category-api-product.png",
            categoryId
        ));
    }

    private void deleteTestData() {
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
    }
}
