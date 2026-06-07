package gift.product.service.admin;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductCommand;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.CreateAdminProductUseCase;
import gift.product.usecase.DeleteAdminProductUseCase;
import gift.product.usecase.GetAdminProductUseCase;
import gift.product.usecase.GetAdminProductsUseCase;
import gift.product.usecase.GetProductFormCategoriesUseCase;
import gift.product.usecase.UpdateAdminProductUseCase;
import gift.product.vo.ProductName;
import gift.support.AbstractMysqlServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminProductUseCaseServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_CATEGORY_PREFIX = "aps-category-";
    private static final String TEST_PRODUCT_PREFIX = "aps-";

    @Autowired
    private GetAdminProductsUseCase getAdminProductsUseCase;

    @Autowired
    private GetAdminProductUseCase getAdminProductUseCase;

    @Autowired
    private CreateAdminProductUseCase createAdminProductUseCase;

    @Autowired
    private UpdateAdminProductUseCase updateAdminProductUseCase;

    @Autowired
    private DeleteAdminProductUseCase deleteAdminProductUseCase;

    @Autowired
    private GetProductFormCategoriesUseCase getProductFormCategoriesUseCase;

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
    void getAdminProductsReturnsProducts() {
        Category category = saveCategory("list");
        Product product = productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + "list",
            1_000,
            "https://example.com/admin-product-list.png",
            category.getId()
        ));

        assertThat(getAdminProductsUseCase.execute())
            .extracting(Product::getId)
            .contains(product.getId());
    }

    @Test
    void getProductFormCategoriesReturnsCategories() {
        Category category = saveCategory("form");

        assertThat(getProductFormCategoriesUseCase.execute())
            .extracting(Category::getId)
            .contains(category.getId());
    }

    @Test
    void getAdminProductReturnsProduct() {
        Category category = saveCategory("get");
        Product product = productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + "get",
            1_000,
            "https://example.com/admin-product-get.png",
            category.getId()
        ));

        assertThat(getAdminProductUseCase.execute(product.getId()))
            .isPresent()
            .get()
            .extracting(Product::getName)
            .isEqualTo(product.getName());
    }

    @Test
    void createAdminProductPersistsProduct() {
        Category category = saveCategory("create");

        Product created = createAdminProductUseCase.execute(productCommand("create", category.getId()));

        assertThat(created.getId()).isNotNull();
        assertThat(productRepository.findById(created.getId()))
            .isPresent()
            .get()
            .extracting(Product::getName)
            .isEqualTo(TEST_PRODUCT_PREFIX + "create");
    }

    @Test
    void createAdminProductRejectsMissingCategory() {
        ProductCommand command = productCommand("miss", Long.MAX_VALUE);

        assertThatThrownBy(() -> createAdminProductUseCase.execute(command))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("카테고리가 존재하지 않습니다.");
    }

    @Test
    void updateAdminProductUpdatesProduct() {
        Category originalCategory = saveCategory("update-original");
        Category nextCategory = saveCategory("update-next");
        Product product = productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + "before",
            1_000,
            "https://example.com/admin-product-before.png",
            originalCategory.getId()
        ));

        Product updated = updateAdminProductUseCase.execute(
            product.getId(),
            productCommand("after", nextCategory.getId())
        );

        assertThat(updated.getName()).isEqualTo(TEST_PRODUCT_PREFIX + "after");
        assertThat(updated.getCategoryId()).isEqualTo(nextCategory.getId());
        assertThat(productRepository.findById(product.getId()))
            .isPresent()
            .get()
            .extracting(Product::getName)
            .isEqualTo(TEST_PRODUCT_PREFIX + "after");
    }

    @Test
    void deleteAdminProductDeletesProduct() {
        Category category = saveCategory("delete");
        Product product = productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + "delete",
            1_000,
            "https://example.com/admin-product-delete.png",
            category.getId()
        ));

        deleteAdminProductUseCase.execute(product.getId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    private Category saveCategory(String suffix) {
        return categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#123456",
            "https://example.com/admin-category-" + suffix + ".png",
            "admin product service category " + suffix
        ));
    }

    private ProductCommand productCommand(String suffix, Long categoryId) {
        return new ProductCommand(
            ProductName.allowingKakao(TEST_PRODUCT_PREFIX + suffix),
            2_000,
            "https://example.com/admin-product-" + suffix + ".png",
            categoryId
        );
    }

    private void deleteTestData() {
        jdbcTemplate.update("delete from product where name like ?", TEST_PRODUCT_PREFIX + "%");
        jdbcTemplate.update("delete from category where name like ?", TEST_CATEGORY_PREFIX + "%");
    }
}
