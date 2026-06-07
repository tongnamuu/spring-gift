package gift.order.service;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.order.controller.OrderResponse;
import gift.order.domain.Order;
import gift.order.domain.OrderRepository;
import gift.order.usecase.CreateOrderUseCase;
import gift.order.usecase.OrderCommand;
import gift.product.dto.OptionResponse;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.OptionCommand;
import gift.product.usecase.CreateOptionUseCase;
import gift.product.vo.OptionName;
import gift.support.AbstractMysqlServiceTest;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class OrderConcurrencyServiceTest extends AbstractMysqlServiceTest {
    private static final String TEST_EMAIL_PREFIX = "order-concurrency-";
    private static final String TEST_CATEGORY_PREFIX = "oc-cat-";
    private static final String TEST_PRODUCT_PREFIX = "ocp-";
    private static final int REQUEST_COUNT = 16;

    @Autowired(required = false)
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CreateOptionUseCase createOptionUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        deleteTestData();
    }

    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    @Test
    void concurrentOrdersDoNotOversellOptionStock() throws Exception {
        assertOrderServiceExists();
        List<Member> members = saveMembers("stock", 100000, REQUEST_COUNT);
        OptionResponse option = saveOption("stock", 1000, 1);

        List<CreateOrderResult> results = createOrdersConcurrently(members, option);
        List<Throwable> failures = failures(results);

        assertThat(successCount(results)).isEqualTo(1L);
        assertThat(failures).hasSize(REQUEST_COUNT - 1);
        assertThat(failures)
            .allSatisfy(this::assertOrderFailure);
        assertThat(countOrdersByOption(option.id())).isEqualTo(1L);
        assertThat(findOptionQuantity(option.id())).isZero();
        assertThat(sumMemberPoint(TEST_EMAIL_PREFIX + "stock-%")).isEqualTo((REQUEST_COUNT * 100000) - 1000);
    }

    @Test
    void concurrentOrdersDoNotOverspendMemberPoint() throws Exception {
        assertOrderServiceExists();
        Member member = saveMember("point", 1000);
        List<OptionResponse> options = saveOptions("point", 1000, 1, REQUEST_COUNT);

        List<CreateOrderResult> results = createOrdersConcurrently(member, options);
        List<Throwable> failures = failures(results);

        assertThat(successCount(results)).isEqualTo(1L);
        assertThat(failures).hasSize(REQUEST_COUNT - 1);
        assertThat(failures)
            .allSatisfy(this::assertOrderFailure);
        assertThat(countOrdersByMember(member.getId())).isEqualTo(1L);
        assertThat(sumOptionQuantity(TEST_PRODUCT_PREFIX + "point-%")).isEqualTo(REQUEST_COUNT - 1);
        assertThat(findMemberPoint(member.getId())).isZero();
    }

    @Test
    void concurrentOrderAndOptionDeleteConflictOnProductOptimisticLock() throws Exception {
        Member member = saveMember("optchg", 100000);
        Product product = saveProduct("optchg", 1000);
        OptionResponse orderedOption = saveOption(product, "주문 옵션 optchg", 10);
        OptionResponse deletedOption = saveOption(product, "삭제 옵션 optchg", 10);
        long initialVersion = findProductVersion(product.getId());

        List<ConcurrentMutationResult> results = runOrderStockChangeAndOptionDeleteConcurrently(
            member.getId(),
            product.getId(),
            orderedOption.id(),
            deletedOption.id()
        );
        List<Throwable> failures = mutationFailures(results);

        assertThat(mutationSuccessCount(results)).isEqualTo(1L);
        assertThat(failures).hasSize(1);
        assertThat(isOptimisticLockFailure(failures.get(0))).isTrue();
        assertThat(findProductVersion(product.getId())).isEqualTo(initialVersion + 1);
    }

    private void assertOrderFailure(Throwable failure) {
        assertThat(failure).isInstanceOfAny(
            IllegalArgumentException.class,
            ConcurrencyFailureException.class
        );
    }

    private void assertOrderServiceExists() {
        assertThat(createOrderUseCase)
            .as("CreateOrderUseCase service implementation is required before order concurrency can be protected.")
            .isNotNull();
    }

    private List<CreateOrderResult> createOrdersConcurrently(Member member, OptionResponse option, int requestCount)
        throws Exception {
        return createOrdersConcurrently(
            IntStream.range(0, requestCount)
                .mapToObj(ignored -> member)
                .toList(),
            IntStream.range(0, requestCount)
                .mapToObj(ignored -> option)
                .toList()
        );
    }

    private List<CreateOrderResult> createOrdersConcurrently(List<Member> members, OptionResponse option)
        throws Exception {
        return createOrdersConcurrently(
            members,
            IntStream.range(0, members.size())
                .mapToObj(ignored -> option)
                .toList()
        );
    }

    private List<CreateOrderResult> createOrdersConcurrently(Member member, List<OptionResponse> options)
        throws Exception {
        return createOrdersConcurrently(
            IntStream.range(0, options.size())
                .mapToObj(ignored -> member)
                .toList(),
            options
        );
    }

    private List<CreateOrderResult> createOrdersConcurrently(List<Member> members, List<OptionResponse> options)
        throws Exception {
        assertThat(options).hasSameSizeAs(members);
        ExecutorService executorService = Executors.newFixedThreadPool(members.size());
        CountDownLatch ready = new CountDownLatch(members.size());
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<CreateOrderResult>> futures = IntStream.range(0, members.size())
                .mapToObj(index -> executorService.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out while waiting to start concurrent order requests.");
                    }
                    try {
                        Member member = members.get(index);
                        OptionResponse option = options.get(index);
                        OrderCommand request = new OrderCommand(option.id(), 1, "동시 주문");
                        OrderResponse response = createOrderUseCase.execute(member.getId(), request);
                        return CreateOrderResult.ok(response);
                    } catch (RuntimeException e) {
                        return CreateOrderResult.failure(e);
                    }
                }))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<CreateOrderResult> results = new ArrayList<>();
            for (Future<CreateOrderResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private List<Member> saveMembers(String suffix, int point, int count) {
        return IntStream.range(0, count)
            .mapToObj(index -> saveMember(suffix + "-" + index, point))
            .toList();
    }

    private List<OptionResponse> saveOptions(String suffix, int productPrice, int quantity, int count) {
        return IntStream.range(0, count)
            .mapToObj(index -> saveOption(suffix + "-" + index, productPrice, quantity))
            .toList();
    }

    private Member saveMember(String suffix, int point) {
        Member member = new Member(TEST_EMAIL_PREFIX + suffix + "@example.com", "password123");
        member.chargePoint(point);
        return memberRepository.save(member);
    }

    private OptionResponse saveOption(String suffix, int productPrice, int quantity) {
        Product product = saveProduct(suffix, productPrice);
        return saveOption(product, "기본 옵션 " + suffix, quantity);
    }

    private Product saveProduct(String suffix, int productPrice) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#123456",
            "https://example.com/order-category.png",
            "order concurrency test category"
        ));
        return productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + suffix,
            productPrice,
            "https://example.com/order-product.png",
            category.getId()
        ));
    }

    private OptionResponse saveOption(Product product, String name, int quantity) {
        return createOptionUseCase.execute(
            product.getId(),
            new OptionCommand(new OptionName(name), quantity)
        );
    }

    private List<ConcurrentMutationResult> runOrderStockChangeAndOptionDeleteConcurrently(
        Long memberId,
        Long productId,
        Long orderedOptionId,
        Long deletedOptionId
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ConcurrentMutationResult> orderFuture = executorService.submit(
                () -> orderAfterConcurrentLoad(memberId, orderedOptionId, ready, start)
            );
            Future<ConcurrentMutationResult> deleteFuture = executorService.submit(
                () -> deleteOptionAfterConcurrentLoad(productId, deletedOptionId, ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return List.of(
                orderFuture.get(10, TimeUnit.SECONDS),
                deleteFuture.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executorService.shutdownNow();
        }
    }

    private ConcurrentMutationResult orderAfterConcurrentLoad(
        Long memberId,
        Long optionId,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Member member = memberRepository.findById(memberId).orElseThrow();
                Product product = productRepository.findByOptionId(optionId).orElseThrow();
                ready.countDown();
                await(start);

                Option option = product.subtractOptionQuantity(optionId, 1);
                member.deductPoint(product.getPrice());
                orderRepository.save(new Order(
                    product.getId(),
                    option.getId(),
                    member.getId(),
                    product.getName(),
                    option.getName(),
                    product.getPrice(),
                    product.getImageUrl(),
                    1,
                    "옵션 변경 동시 주문"
                ));
            });
            return ConcurrentMutationResult.success();
        } catch (Throwable failure) {
            return ConcurrentMutationResult.failure(failure);
        }
    }

    private ConcurrentMutationResult deleteOptionAfterConcurrentLoad(
        Long productId,
        Long optionId,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Product product = productRepository.findById(productId).orElseThrow();
                ready.countDown();
                await(start);

                product.removeOption(optionId);
                productRepository.save(product);
            });
            return ConcurrentMutationResult.success();
        } catch (Throwable failure) {
            return ConcurrentMutationResult.failure(failure);
        }
    }

    private void await(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to start concurrent mutations.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to start concurrent mutations.", e);
        }
    }

    private long successCount(List<CreateOrderResult> results) {
        return results.stream()
            .filter(CreateOrderResult::succeeded)
            .count();
    }

    private List<Throwable> failures(List<CreateOrderResult> results) {
        return results.stream()
            .filter(result -> !result.succeeded())
            .map(CreateOrderResult::failure)
            .toList();
    }

    private long countOrders(Long memberId, Long optionId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from orders where member_id = ? and option_id = ?",
            Long.class,
            memberId,
            optionId
        );
    }

    private long countOrdersByMember(Long memberId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from orders where member_id = ?",
            Long.class,
            memberId
        );
    }

    private long countOrdersByOption(Long optionId) {
        return jdbcTemplate.queryForObject(
            "select count(*) from orders where option_id = ?",
            Long.class,
            optionId
        );
    }

    private int findOptionQuantity(Long optionId) {
        return jdbcTemplate.queryForObject(
            "select quantity from options where id = ?",
            Integer.class,
            optionId
        );
    }

    private int findMemberPoint(Long memberId) {
        return jdbcTemplate.queryForObject(
            "select point from member where id = ?",
            Integer.class,
            memberId
        );
    }

    private long findProductVersion(Long productId) {
        return jdbcTemplate.queryForObject(
            "select version from product where id = ?",
            Long.class,
            productId
        );
    }

    private int sumOptionQuantity(String productNamePattern) {
        Integer sum = jdbcTemplate.queryForObject(
            """
                select coalesce(sum(o.quantity), 0)
                from options o
                inner join product p on p.id = o.product_id
                where p.name like ?
                """,
            Integer.class,
            productNamePattern
        );
        return sum == null ? 0 : sum;
    }

    private int sumMemberPoint(String emailPattern) {
        Integer sum = jdbcTemplate.queryForObject(
            "select coalesce(sum(point), 0) from member where email like ?",
            Integer.class,
            emailPattern
        );
        return sum == null ? 0 : sum;
    }

    private long mutationSuccessCount(List<ConcurrentMutationResult> results) {
        return results.stream()
            .filter(ConcurrentMutationResult::succeeded)
            .count();
    }

    private List<Throwable> mutationFailures(List<ConcurrentMutationResult> results) {
        return results.stream()
            .filter(result -> !result.succeeded())
            .map(ConcurrentMutationResult::failure)
            .toList();
    }

    private boolean isOptimisticLockFailure(Throwable failure) {
        return containsCause(failure, OptimisticLockingFailureException.class)
            || containsCause(failure, OptimisticLockException.class);
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

    private void deleteTestData() {
        jdbcTemplate.update(
            "delete from orders where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
        );
        jdbcTemplate.update(
            "delete from orders where option_id in (select id from options where product_id in (select id from product where name like ?))",
            TEST_PRODUCT_PREFIX + "%"
        );
        jdbcTemplate.update(
            "delete from wish where member_id in (select id from member where email like ?)",
            TEST_EMAIL_PREFIX + "%"
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

    private record CreateOrderResult(boolean succeeded, OrderResponse response, Throwable failure) {
        private static CreateOrderResult ok(OrderResponse response) {
            return new CreateOrderResult(true, response, null);
        }

        private static CreateOrderResult failure(Throwable failure) {
            return new CreateOrderResult(false, null, failure);
        }
    }

    private record ConcurrentMutationResult(boolean succeeded, Throwable failure) {
        private static ConcurrentMutationResult success() {
            return new ConcurrentMutationResult(true, null);
        }

        private static ConcurrentMutationResult failure(Throwable failure) {
            return new ConcurrentMutationResult(false, failure);
        }
    }
}
