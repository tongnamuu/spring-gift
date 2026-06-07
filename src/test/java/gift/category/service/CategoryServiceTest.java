package gift.category.service;

import gift.category.controller.CategoryRequest;
import gift.category.controller.CategoryResponse;
import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.category.query.GetCategoriesService;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_CATEGORY_PREFIX = "service-test-";
    private static final String TEST_PRODUCT_PREFIX = "cat-del-";

    @Autowired
    private CreateCategoryService createCategoryService;

    @Autowired
    private GetCategoriesService getCategoriesService;

    @Autowired
    private UpdateCategoryService updateCategoryService;

    @Autowired
    private DeleteCategoryService deleteCategoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        deleteTestCategories();
    }

    @AfterEach
    void tearDown() {
        deleteTestCategories();
    }

    @Test
    void createCategoryPersistsCategory() {
        CategoryRequest request = new CategoryRequest(
            TEST_CATEGORY_PREFIX + "create",
            "#123456",
            "https://example.com/service-create.png",
            "created by service test"
        );

        CategoryResponse response = createCategoryService.execute(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.color()).isEqualTo(request.color());
        assertThat(response.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(response.description()).isEqualTo(request.description());

        categoryRepository.flush();

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select id, name, color, image_url, description from category where id = ?",
            response.id()
        );
        assertThat(((Number) persisted.get("id")).longValue()).isEqualTo(response.id());
        assertThat(persisted.get("name")).isEqualTo(request.name());
        assertThat(persisted.get("color")).isEqualTo(request.color());
        assertThat(persisted.get("image_url")).isEqualTo(request.imageUrl());
        assertThat(persisted.get("description")).isEqualTo(request.description());
    }

    @Test
    void getCategoriesReturnsPersistedCategories() {
        Category first = saveCategory(TEST_CATEGORY_PREFIX + "list-first");
        Category second = saveCategory(TEST_CATEGORY_PREFIX + "list-second");

        List<CategoryResponse> responses = getCategoriesService.execute();

        assertThat(responses)
            .extracting(CategoryResponse::id)
            .contains(first.getId(), second.getId());
        assertThat(responses)
            .extracting(CategoryResponse::name)
            .contains(first.getName(), second.getName());
    }

    @Test
    void updateCategoryChangesPersistedCategory() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "update-before");
        CategoryRequest request = new CategoryRequest(
            TEST_CATEGORY_PREFIX + "update-after",
            "#654321",
            "https://example.com/service-update.png",
            "updated by service test"
        );

        Optional<CategoryResponse> response = updateCategoryService.execute(category.getId(), request);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().id()).isEqualTo(category.getId());
        assertThat(response.orElseThrow().name()).isEqualTo(request.name());

        Category persisted = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo(request.name());
        assertThat(persisted.getColor()).isEqualTo(request.color());
        assertThat(persisted.getImageUrl()).isEqualTo(request.imageUrl());
        assertThat(persisted.getDescription()).isEqualTo(request.description());
    }

    @Test
    void updateCategoryReturnsEmptyWhenCategoryDoesNotExist() {
        CategoryRequest request = new CategoryRequest(
            TEST_CATEGORY_PREFIX + "missing-update",
            "#111111",
            "https://example.com/missing-update.png",
            "missing update"
        );

        Optional<CategoryResponse> response = updateCategoryService.execute(Long.MAX_VALUE, request);

        assertThat(response).isEmpty();
    }

    @Test
    void deleteCategoryRemovesPersistedCategory() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "delete");

        deleteCategoryService.execute(category.getId());

        assertThat(categoryRepository.existsById(category.getId())).isFalse();
    }

    @Test
    void deleteCategoryKeepsProductWithMissingCategoryReference() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "delete-referenced");
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "ref", category.getId());

        deleteCategoryService.execute(category.getId());

        assertThat(categoryRepository.existsById(category.getId())).isFalse();
        Product persistedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persistedProduct.getCategoryId()).isEqualTo(category.getId());
    }

    private Category saveCategory(String name) {
        return categoryRepository.save(new Category(
            name,
            "#ABCDEF",
            "https://example.com/service-test.png",
            "service test category"
        ));
    }

    private Product saveProduct(String name, Long categoryId) {
        return productRepository.save(new Product(
            name,
            1_000,
            "https://example.com/category-product.png",
            categoryId
        ));
    }

    private void deleteTestCategories() {
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
    }
}
