package gift.order;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.OptionRepository;
import gift.product.repository.ProductRepository;
import gift.support.AbstractMysqlServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

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
    void concurrentOrdersDoNotOversellOptionStock() throws Exception {
        assertOrderServiceExists();
        List<Member> members = saveMembers("stock", 100000, REQUEST_COUNT);
        Option option = saveOption("stock", 1000, 1);

        List<CreateOrderResult> results = createOrdersConcurrently(members, option);
        List<Throwable> failures = failures(results);

        assertThat(successCount(results)).isEqualTo(1L);
        assertThat(failures).hasSize(REQUEST_COUNT - 1);
        assertThat(failures)
            .allSatisfy(this::assertOrderFailure);
        assertThat(countOrdersByOption(option.getId())).isEqualTo(1L);
        assertThat(findOptionQuantity(option.getId())).isZero();
        assertThat(sumMemberPoint(TEST_EMAIL_PREFIX + "stock-%")).isEqualTo((REQUEST_COUNT * 100000) - 1000);
    }

    @Test
    void concurrentOrdersDoNotOverspendMemberPoint() throws Exception {
        assertOrderServiceExists();
        Member member = saveMember("point", 1000);
        List<Option> options = saveOptions("point", 1000, 1, REQUEST_COUNT);

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

    private List<CreateOrderResult> createOrdersConcurrently(Member member, Option option, int requestCount)
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

    private List<CreateOrderResult> createOrdersConcurrently(List<Member> members, Option option)
        throws Exception {
        return createOrdersConcurrently(
            members,
            IntStream.range(0, members.size())
                .mapToObj(ignored -> option)
                .toList()
        );
    }

    private List<CreateOrderResult> createOrdersConcurrently(Member member, List<Option> options)
        throws Exception {
        return createOrdersConcurrently(
            IntStream.range(0, options.size())
                .mapToObj(ignored -> member)
                .toList(),
            options
        );
    }

    private List<CreateOrderResult> createOrdersConcurrently(List<Member> members, List<Option> options)
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
                        Option option = options.get(index);
                        OrderRequest request = new OrderRequest(option.getId(), 1, "동시 주문");
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

    private List<Option> saveOptions(String suffix, int productPrice, int quantity, int count) {
        return IntStream.range(0, count)
            .mapToObj(index -> saveOption(suffix + "-" + index, productPrice, quantity))
            .toList();
    }

    private Member saveMember(String suffix, int point) {
        Member member = new Member(TEST_EMAIL_PREFIX + suffix + "@example.com", "password123");
        member.chargePoint(point);
        return memberRepository.save(member);
    }

    private Option saveOption(String suffix, int productPrice, int quantity) {
        Category category = categoryRepository.save(new Category(
            TEST_CATEGORY_PREFIX + suffix,
            "#123456",
            "https://example.com/order-category.png",
            "order concurrency test category"
        ));
        Product product = productRepository.save(new Product(
            TEST_PRODUCT_PREFIX + suffix,
            productPrice,
            "https://example.com/order-product.png",
            category.getId()
        ));
        return optionRepository.save(new Option(product, "기본 옵션 " + suffix, quantity));
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
}
