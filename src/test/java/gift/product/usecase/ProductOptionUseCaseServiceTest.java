package gift.product.usecase;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.dto.OptionCommand;
import gift.product.dto.OptionRequest;
import gift.product.dto.OptionResponse;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.vo.OptionName;
import gift.support.AbstractMysqlServiceTest;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static gift.product.support.ProductFixtures.product;

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

        OptionResponse response = createOptionUseCase.execute(product.getId(), optionCommand(request));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.quantity()).isEqualTo(request.quantity());

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
        OptionResponse first = saveOption(product, TEST_OPTION_PREFIX + "list-first", 10);
        OptionResponse second = saveOption(product, TEST_OPTION_PREFIX + "list-second", 20);

        Optional<List<OptionResponse>> response = getOptionsUseCase.execute(product.getId());

        assertThat(response).isPresent();
        assertThat(response.orElseThrow())
            .extracting(OptionResponse::id)
            .contains(first.id(), second.id());
        assertThat(response.orElseThrow())
            .extracting(OptionResponse::name)
            .contains(first.name(), second.name());
    }

    @Test
    void createOptionRejectsDuplicateNameInSameProduct() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "duplicate");
        saveOption(product, TEST_OPTION_PREFIX + "duplicate", 10);
        OptionRequest request = new OptionRequest(TEST_OPTION_PREFIX + "duplicate", 20);

        assertThatThrownBy(() -> createOptionUseCase.execute(product.getId(), optionCommand(request)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 옵션명입니다.");
    }

    @Test
    void concurrentDuplicateOptionNamesCreateOnlyOneOption() throws Exception {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "cdup");
        OptionRequest request = new OptionRequest(TEST_OPTION_PREFIX + "concurrent-duplicate", 10);
        long initialVersion = findProductVersion(product.getId());

        List<CreateOptionResult> results = createOptionsConcurrently(product.getId(), request, 2);

        assertThat(createSuccessCount(results)).isEqualTo(1L);
        assertThat(createFailures(results)).hasSize(1)
            .allSatisfy(this::assertOptimisticLockFailure);
        assertThat(countOptionsByProductAndName(product.getId(), request.name())).isEqualTo(1L);
        assertThat(findProductVersion(product.getId())).isEqualTo(initialVersion + 1);
    }

    @Test
    void deleteOptionRemovesOptionWhenMoreThanOneOptionExists() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "delete");
        OptionResponse first = saveOption(product, TEST_OPTION_PREFIX + "delete-first", 10);
        OptionResponse second = saveOption(product, TEST_OPTION_PREFIX + "delete-second", 20);

        deleteOptionUseCase.execute(product.getId(), first.id());

        assertThat(optionExists(first.id())).isFalse();
        assertThat(optionExists(second.id())).isTrue();
    }

    @Test
    void deleteOptionRejectsRemovingLastOption() {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "last");
        OptionResponse option = saveOption(product, TEST_OPTION_PREFIX + "last", 10);

        assertThatThrownBy(() -> deleteOptionUseCase.execute(product.getId(), option.id()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        assertThat(optionExists(option.id())).isTrue();
    }

    @Test
    void concurrentOptionDeletesDoNotRemoveEveryOption() throws Exception {
        Product product = saveProduct(TEST_PRODUCT_PREFIX + "c-del");
        OptionResponse first = saveOption(product, TEST_OPTION_PREFIX + "concurrent-first", 10);
        OptionResponse second = saveOption(product, TEST_OPTION_PREFIX + "concurrent-second", 20);

        List<DeleteOptionResult> results = deleteOptionsConcurrently(
            product.getId(),
            List.of(first.id(), second.id())
        );

        assertThat(successCount(results)).isEqualTo(1L);
        assertThat(failures(results)).hasSize(1)
            .allSatisfy(this::assertDeleteFailure);
        assertThat(countOptionsByProduct(product.getId())).isEqualTo(1L);
    }

    private Product saveProduct(String name) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + name,
            "#654321",
            "https://example.com/category.png",
            "product option service test category"
        ));
        return productRepository.save(product(
            name,
            10000,
            "https://example.com/product.png",
            category.getId()
        ));
    }

    private OptionResponse saveOption(Product product, String name, int quantity) {
        return createOptionUseCase.execute(product.getId(), optionCommand(name, quantity));
    }

    private List<CreateOptionResult> createOptionsConcurrently(
        Long productId,
        OptionRequest request,
        int requestCount
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<CreateOptionResult>> futures = java.util.stream.IntStream.range(0, requestCount)
                .mapToObj(ignored -> executorService.submit(() -> createOptionAfterStart(productId, request, ready, start)))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return futures.stream()
                .map(this::getCreateResult)
                .toList();
        } finally {
            executorService.shutdownNow();
        }
    }

    private CreateOptionResult createOptionAfterStart(
        Long productId,
        OptionRequest request,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        ready.countDown();
        await(start);
        try {
            OptionResponse response = createOptionUseCase.execute(productId, optionCommand(request));
            return CreateOptionResult.success(response);
        } catch (Throwable failure) {
            return CreateOptionResult.failure(failure);
        }
    }

    private OptionCommand optionCommand(OptionRequest request) {
        return optionCommand(request.name(), request.quantity());
    }

    private OptionCommand optionCommand(String name, int quantity) {
        return new OptionCommand(new OptionName(name), quantity);
    }

    private List<DeleteOptionResult> deleteOptionsConcurrently(Long productId, List<Long> optionIds) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(optionIds.size());
        CountDownLatch ready = new CountDownLatch(optionIds.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<DeleteOptionResult>> futures = optionIds.stream()
                .map(optionId -> executorService.submit(() -> deleteOptionAfterStart(productId, optionId, ready, start)))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return futures.stream()
                .map(this::getResult)
                .toList();
        } finally {
            executorService.shutdownNow();
        }
    }

    private DeleteOptionResult deleteOptionAfterStart(
        Long productId,
        Long optionId,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        ready.countDown();
        await(start);
        try {
            deleteOptionUseCase.execute(productId, optionId);
            return DeleteOptionResult.success();
        } catch (Throwable failure) {
            return DeleteOptionResult.failure(failure);
        }
    }

    private void await(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent option delete.", e);
        }
    }

    private CreateOptionResult getCreateResult(Future<CreateOptionResult> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to collect concurrent option create result.", e);
        }
    }

    private DeleteOptionResult getResult(Future<DeleteOptionResult> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to collect concurrent option delete result.", e);
        }
    }

    private long successCount(List<DeleteOptionResult> results) {
        return results.stream()
            .filter(DeleteOptionResult::succeeded)
            .count();
    }

    private List<Throwable> failures(List<DeleteOptionResult> results) {
        return results.stream()
            .map(DeleteOptionResult::failure)
            .filter(failure -> failure != null)
            .toList();
    }

    private long createSuccessCount(List<CreateOptionResult> results) {
        return results.stream()
            .filter(CreateOptionResult::succeeded)
            .count();
    }

    private List<Throwable> createFailures(List<CreateOptionResult> results) {
        return results.stream()
            .map(CreateOptionResult::failure)
            .filter(failure -> failure != null)
            .toList();
    }

    private void assertOptimisticLockFailure(Throwable failure) {
        assertThat(containsCause(failure, OptimisticLockingFailureException.class)
            || containsCause(failure, OptimisticLockException.class)
            || containsProductVersionLockFailure(failure))
            .isTrue();
    }

    private void assertDeleteFailure(Throwable failure) {
        assertThat(failure).isInstanceOfAny(
            IllegalArgumentException.class,
            ConcurrencyFailureException.class
        );
    }

    private boolean containsCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsProductVersionLockFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof CannotAcquireLockException && isProductVersionUpdateFailure(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isProductVersionUpdateFailure(Throwable failure) {
        String message = failure.getMessage();
        return message != null
            && message.contains("update product")
            && message.contains("version=?");
    }

    private long countOptionsByProduct(Long productId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from options where product_id = ?",
            Long.class,
            productId
        );
    }

    private long findProductVersion(Long productId) {
        return jdbcTemplate.queryForObject(
            "select version from product where id = ?",
            Long.class,
            productId
        );
    }

    private long countOptionsByProductAndName(Long productId, String name) {
        return jdbcTemplate.queryForObject(
            "select count(*) from options where product_id = ? and name = ?",
            Long.class,
            productId,
            name
        );
    }

    private boolean optionExists(Long optionId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from options where id = ?",
            Long.class,
            optionId
        ) > 0;
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

    private record CreateOptionResult(boolean succeeded, OptionResponse response, Throwable failure) {
        private static CreateOptionResult success(OptionResponse response) {
            return new CreateOptionResult(true, response, null);
        }

        private static CreateOptionResult failure(Throwable failure) {
            return new CreateOptionResult(false, null, failure);
        }
    }

    private record DeleteOptionResult(boolean succeeded, Throwable failure) {
        private static DeleteOptionResult success() {
            return new DeleteOptionResult(true, null);
        }

        private static DeleteOptionResult failure(Throwable failure) {
            return new DeleteOptionResult(false, failure);
        }
    }
}
