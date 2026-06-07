package gift.product.usecase;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.dto.OptionRequest;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlServiceTest;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductUseCaseServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_CATEGORY_PREFIX = "pc-";
    private static final String TEST_PRODUCT_PREFIX = "p-";
    private static final String TEST_EMAIL_PREFIX = "product-service-";

    @Autowired
    private CreateProductUseCase createProductUseCase;

    @Autowired
    private GetProductUseCase getProductUseCase;

    @Autowired
    private GetProductsUseCase getProductsUseCase;

    @Autowired
    private UpdateProductUseCase updateProductUseCase;

    @Autowired
    private DeleteProductUseCase deleteProductUseCase;

    @Autowired
    private CreateOptionUseCase createOptionUseCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WishRepository wishRepository;

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
    void createProductPersistsProduct() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "create");
        ProductRequest request = new ProductRequest(
            TEST_PRODUCT_PREFIX + "create",
            10000,
            "https://example.com/product-create.png",
            category.getId()
        );

        ProductResponse response = createProductUseCase.execute(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.price()).isEqualTo(request.price());
        assertThat(response.imageUrl()).isEqualTo(request.imageUrl());
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.options()).isEmpty();

        productRepository.flush();

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select id, name, price, image_url, category_id from product where id = ?",
            response.id()
        );
        assertThat(((Number) persisted.get("id")).longValue()).isEqualTo(response.id());
        assertThat(persisted.get("name")).isEqualTo(request.name());
        assertThat(((Number) persisted.get("price")).intValue()).isEqualTo(request.price());
        assertThat(persisted.get("image_url")).isEqualTo(request.imageUrl());
        assertThat(((Number) persisted.get("category_id")).longValue()).isEqualTo(category.getId());
    }

    @Test
    void getProductReturnsPersistedProduct() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "get");
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "get", category.getId());
        createOptionUseCase.execute(product.getId(), new OptionRequest("단건 옵션", 10));

        Optional<ProductResponse> response = getProductUseCase.execute(product.getId());

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().id()).isEqualTo(product.getId());
        assertThat(response.orElseThrow().name()).isEqualTo(product.getName());
        assertThat(response.orElseThrow().categoryId()).isEqualTo(product.getCategoryId());
        assertThat(response.orElseThrow().options())
            .singleElement()
            .satisfies(option -> {
                assertThat(option.name()).isEqualTo("단건 옵션");
                assertThat(option.quantity()).isEqualTo(10);
            });
    }

    @Test
    void getProductsReturnsPersistedProducts() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "list");
        Product first = saveProduct(TEST_PRODUCT_PREFIX + "list-first", category.getId());
        Product second = saveProduct(TEST_PRODUCT_PREFIX + "list-second", category.getId());
        createOptionUseCase.execute(first.getId(), new OptionRequest("목록 첫 옵션", 10));
        createOptionUseCase.execute(second.getId(), new OptionRequest("목록 둘 옵션", 20));

        Page<ProductResponse> response = getProductsUseCase.execute(PageRequest.of(0, 20));

        assertThat(response.getContent())
            .extracting(ProductResponse::id)
            .contains(first.getId(), second.getId());
        assertThat(response.getContent())
            .extracting(ProductResponse::name)
            .contains(first.getName(), second.getName());
        ProductResponse firstResponse = findProductResponse(response, first.getId());
        ProductResponse secondResponse = findProductResponse(response, second.getId());
        assertThat(firstResponse.options())
            .singleElement()
            .satisfies(option -> assertThat(option.name()).isEqualTo("목록 첫 옵션"));
        assertThat(secondResponse.options())
            .singleElement()
            .satisfies(option -> assertThat(option.name()).isEqualTo("목록 둘 옵션"));
    }

    @Test
    void updateProductChangesPersistedProduct() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "before");
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "update-before", category.getId());
        Category newCategory = saveCategory(TEST_CATEGORY_PREFIX + "after");
        ProductRequest request = new ProductRequest(
            TEST_PRODUCT_PREFIX + "update-after",
            20000,
            "https://example.com/product-update.png",
            newCategory.getId()
        );

        Optional<ProductResponse> response = updateProductUseCase.execute(product.getId(), request);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().id()).isEqualTo(product.getId());
        assertThat(response.orElseThrow().name()).isEqualTo(request.name());
        assertThat(response.orElseThrow().categoryId()).isEqualTo(newCategory.getId());
        assertThat(response.orElseThrow().options()).isEmpty();

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo(request.name());
        assertThat(persisted.getPrice()).isEqualTo(request.price());
        assertThat(persisted.getImageUrl()).isEqualTo(request.imageUrl());
        assertThat(persisted.getCategoryId()).isEqualTo(newCategory.getId());
    }

    @Test
    void deleteProductRemovesPersistedProduct() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "delete");
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "delete", category.getId());

        deleteProductUseCase.execute(product.getId());

        assertThat(productRepository.existsById(product.getId())).isFalse();
    }

    @Test
    void deleteProductKeepsWishWithMissingProductReference() {
        Category category = saveCategory(TEST_CATEGORY_PREFIX + "delete-wish");
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "delete-wish", category.getId());
        Member member = saveMember("delete-wish");
        Wish wish = wishRepository.save(new Wish(member.getId(), product.getId()));

        deleteProductUseCase.execute(product.getId());

        assertThat(productRepository.existsById(product.getId())).isFalse();
        Map<String, Object> persistedWish = jdbcTemplate.queryForMap(
            "select id, member_id, product_id from wish where id = ?",
            wish.getId()
        );
        assertThat(((Number) persistedWish.get("id")).longValue()).isEqualTo(wish.getId());
        assertThat(((Number) persistedWish.get("member_id")).longValue()).isEqualTo(member.getId());
        assertThat(((Number) persistedWish.get("product_id")).longValue()).isEqualTo(product.getId());
    }

    private Category saveCategory(String name) {
        return categoryRepository.save(new Category(
            name,
            "#123456",
            "https://example.com/category.png",
            "product service test category"
        ));
    }

    private Product saveProduct(String name, Long categoryId) {
        return productRepository.save(new Product(
            name,
            10000,
            "https://example.com/product.png",
            categoryId
        ));
    }

    private Member saveMember(String suffix) {
        return memberRepository.save(new Member(TEST_EMAIL_PREFIX + suffix + "@example.com", "password123"));
    }

    private ProductResponse findProductResponse(Page<ProductResponse> response, Long productId) {
        return response.getContent().stream()
            .filter(productResponse -> productResponse.id().equals(productId))
            .findFirst()
            .orElseThrow();
    }

    private void deleteTestData() {
        jdbcTemplate.update(
            "delete from wish where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
        );
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
        jdbcTemplate.update("delete from member where email like ?", TEST_EMAIL_PREFIX + "%");
    }
}
