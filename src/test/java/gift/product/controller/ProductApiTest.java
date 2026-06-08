package gift.product.controller;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
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

import static gift.product.support.ProductFixtures.product;

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
    void createProductReturnsCreatedWhenPriceIsPositive() {
        Category category = saveCategory("positive");
        ProductRequest request = new ProductRequest(
            TEST_PRODUCT_PREFIX + "positive",
            1,
            "https://example.com/product-positive.png",
            category.getId()
        );

        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
            "/api/products",
            request,
            ProductResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().price()).isEqualTo(1);
        assertThat(findPrice(response.getBody().id())).isEqualTo(1);
    }

    @Test
    void createProductReturnsBadRequestWhenPriceIsZero() {
        Category category = saveCategory("zero");
        String name = TEST_PRODUCT_PREFIX + "zero";
        ProductRequest request = new ProductRequest(
            name,
            0,
            "https://example.com/product-zero.png",
            category.getId()
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/products",
            request,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("상품 가격은 1 이상이어야 합니다.");
        assertThat(countProductsByName(name)).isZero();
    }

    @Test
    void createProductReturnsBadRequestWhenPriceIsNegative() {
        Category category = saveCategory("negative");
        String name = TEST_PRODUCT_PREFIX + "negative";
        ProductRequest request = new ProductRequest(
            name,
            -1,
            "https://example.com/product-negative.png",
            category.getId()
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/products",
            request,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("상품 가격은 1 이상이어야 합니다.");
        assertThat(countProductsByName(name)).isZero();
    }

    @Test
    void createProductReturnsBadRequestWhenCategoryDoesNotExist() {
        Long missingCategoryId = Long.MAX_VALUE;
        String name = TEST_PRODUCT_PREFIX + "misscat";
        ProductRequest request = new ProductRequest(
            name,
            20_000,
            "https://example.com/product-missing-category.png",
            missingCategoryId
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/products",
            request,
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("카테고리가 존재하지 않습니다. id=" + missingCategoryId);
        assertThat(countProductsByName(name)).isZero();
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
        return productRepository.save(product(
            TEST_PRODUCT_PREFIX + suffix,
            10_000,
            "https://example.com/product-api.png",
            categoryId
        ));
    }

    private int findPrice(Long productId) {
        return jdbcTemplate.queryForObject(
            "select price from product where id = ?",
            Integer.class,
            productId
        );
    }

    private long countProductsByName(String name) {
        return jdbcTemplate.queryForObject(
            "select count(*) from product where name = ?",
            Long.class,
            name
        );
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
