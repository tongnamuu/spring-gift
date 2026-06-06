package gift.product.usecase;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.product.dto.OptionRequest;
import gift.product.dto.OptionResponse;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.OptionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductOptionUseCaseServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_CATEGORY_PREFIX = "poc-";
    private static final String TEST_PRODUCT_PREFIX = "po-";
    private static final String TEST_OPTION_PREFIX = "oo-";

    @Autowired
    private GetOptionsUseCase getOptionsUseCase;

    @Autowired
    private CreateOptionUseCase createOptionUseCase;

    @Autowired
    private DeleteOptionUseCase deleteOptionUseCase;

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
    void createOptionPersistsOptionForProduct() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "create");
        OptionRequest request = new OptionRequest(TEST_OPTION_PREFIX + "create", 10);

        OptionResponse response = createOptionUseCase.execute(product.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.quantity()).isEqualTo(request.quantity());

        optionRepository.flush();

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
            "select id, product_id, name, quantity from options where id = ?",
            response.id()
        );
        assertThat(((Number) persisted.get("id")).longValue()).isEqualTo(response.id());
        assertThat(((Number) persisted.get("product_id")).longValue()).isEqualTo(product.getId());
        assertThat(persisted.get("name")).isEqualTo(request.name());
        assertThat(((Number) persisted.get("quantity")).intValue()).isEqualTo(request.quantity());
    }

    @Test
    void getOptionsReturnsPersistedProductOptions() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "list");
        Option first = saveOption(product, TEST_OPTION_PREFIX + "list-first", 10);
        Option second = saveOption(product, TEST_OPTION_PREFIX + "list-second", 20);

        Optional<List<OptionResponse>> response = getOptionsUseCase.execute(product.getId());

        assertThat(response).isPresent();
        assertThat(response.orElseThrow())
            .extracting(OptionResponse::id)
            .contains(first.getId(), second.getId());
        assertThat(response.orElseThrow())
            .extracting(OptionResponse::name)
            .contains(first.getName(), second.getName());
    }

    @Test
    void createOptionRejectsDuplicateNameInSameProduct() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "duplicate");
        saveOption(product, TEST_OPTION_PREFIX + "duplicate", 10);
        OptionRequest request = new OptionRequest(TEST_OPTION_PREFIX + "duplicate", 20);

        assertThatThrownBy(() -> createOptionUseCase.execute(product.getId(), request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 옵션명입니다.");
    }

    @Test
    void deleteOptionRemovesOptionWhenMoreThanOneOptionExists() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "delete");
        Option first = saveOption(product, TEST_OPTION_PREFIX + "delete-first", 10);
        Option second = saveOption(product, TEST_OPTION_PREFIX + "delete-second", 20);

        deleteOptionUseCase.execute(product.getId(), first.getId());

        assertThat(optionRepository.existsById(first.getId())).isFalse();
        assertThat(optionRepository.existsById(second.getId())).isTrue();
    }

    @Test
    void deleteOptionRejectsRemovingLastOption() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "last");
        Option option = saveOption(product, TEST_OPTION_PREFIX + "last", 10);

        assertThatThrownBy(() -> deleteOptionUseCase.execute(product.getId(), option.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        assertThat(optionRepository.existsById(option.getId())).isTrue();
    }

    private Product saveProduct(String name) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + name,
            "#654321",
            "https://example.com/category.png",
            "product option service test category"
        ));
        return productRepository.save(new Product(
            name,
            10000,
            "https://example.com/product.png",
            category.getId()
        ));
    }

    private Option saveOption(Product product, String name, int quantity) {
        return optionRepository.save(new Option(product, name, quantity));
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
