package gift.product.controller;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static gift.product.support.ProductFixtures.product;

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
    void createProductShowsNegativePriceErrorAndDoesNotPersist() {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + "negative",
            "#123456",
            "https://example.com/admin-category.png",
            "admin product negative price category"
        ));
        String name = TEST_PRODUCT_PREFIX + "negative";

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/admin/products",
            formEntity(
                "name", name,
                "price", "-1",
                "imageUrl", "https://example.com/admin-product-negative.png",
                "categoryId", String.valueOf(category.getId())
            ),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("상품 가격은 1 이상이어야 합니다.");
        assertThat(countProductsByName(name)).isZero();
    }

    @Test
    void productListDisplaysUncategorizedWhenCategoryIsMissing() {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + "missing",
            "#123456",
            "https://example.com/admin-category.png",
            "admin product list category"
        ));
        Product product = productRepository.save(product(
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

    private HttpEntity<MultiValueMap<String, String>> formEntity(String... values) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int index = 0; index < values.length; index += 2) {
            form.add(values[index], values[index + 1]);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<>(form, headers);
    }

    private long countProductsByName(String name) {
        return jdbcTemplate.queryForObject(
            "select count(*) from product where name = ?",
            Long.class,
            name
        );
    }

    private void deleteTestData() {
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
    }
}
