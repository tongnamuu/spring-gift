package gift.product.controller;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductRequest;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ProductApiTest extends AbstractMysqlApiTest {
    private static final String TEST_CATEGORY_PREFIX = "pa-cat-";
    private static final String TEST_PRODUCT_PREFIX = "pa-";

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
    void updateProductReturnsBadRequestWhenCategoryDoesNotExist() {
        Category category = saveCategory("before");
        Product product = saveProduct("update", category.getId());
        Long missingCategoryId = Long.MAX_VALUE;
        ProductRequest request = new ProductRequest(
            TEST_PRODUCT_PREFIX + "updated",
            20_000,
            "https://example.com/product-updated.png",
            missingCategoryId
        );

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/products/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(request),
            String.class,
            product.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("카테고리가 존재하지 않습니다. id=" + missingCategoryId);
        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo(product.getName());
        assertThat(persisted.getCategoryId()).isEqualTo(category.getId());
    }

    @Test
    void updateProductReturnsNotFoundWhenProductDoesNotExist() {
        Category category = saveCategory("target");
        Long missingProductId = Long.MAX_VALUE;
        ProductRequest request = new ProductRequest(
            TEST_PRODUCT_PREFIX + "missing",
            20_000,
            "https://example.com/product-missing.png",
            category.getId()
        );

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/products/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(request),
            String.class,
            missingProductId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Category saveCategory(String suffix) {
        return categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#123456",
            "https://example.com/product-api-category.png",
            "product api test category"
        ));
    }

    private Product saveProduct(String suffix, Long categoryId) {
        return productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + suffix,
            10_000,
            "https://example.com/product-api.png",
            categoryId
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
}
