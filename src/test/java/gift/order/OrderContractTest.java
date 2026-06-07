package gift.order;

import gift.product.entity.Option;
import gift.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderContractTest {
    private static final Long PRODUCT_ID = 7L;
    private static final Long OPTION_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final String PRODUCT_NAME = "2025 햅쌀";
    private static final String OPTION_NAME = "2025년 재배";
    private static final int UNIT_PRICE = 30000;
    private static final String PRODUCT_IMAGE_URL = "https://example.com/2025-rice.png";

    @Test
    void orderKeepsPurchasedSnapshotAndCreationTime() {
        LocalDateTime beforeCreation = LocalDateTime.now();

        Order order = order();

        assertThat(order.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(order.getOptionId()).isEqualTo(OPTION_ID);
        assertThat(order.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(order.getProductName()).isEqualTo(PRODUCT_NAME);
        assertThat(order.getOptionName()).isEqualTo(OPTION_NAME);
        assertThat(order.getUnitPrice()).isEqualTo(UNIT_PRICE);
        assertThat(order.getProductImageUrl()).isEqualTo(PRODUCT_IMAGE_URL);
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getMessage()).isEqualTo("선물 메시지");
        assertThat(order.getOrderDateTime()).isAfterOrEqualTo(beforeCreation);
        assertThat(order.getOrderDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void orderCreationDoesNotChangeOptionStockByItself() {
        Option option = option();

        order();

        assertThat(option.getQuantity()).isEqualTo(5);
    }

    @Test
    void orderResponseRepresentsOrderEntity() {
        Order order = order();
        setId(order, 20L);

        OrderResponse response = OrderResponse.from(order);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.optionId()).isEqualTo(OPTION_ID);
        assertThat(response.productName()).isEqualTo(PRODUCT_NAME);
        assertThat(response.optionName()).isEqualTo(OPTION_NAME);
        assertThat(response.unitPrice()).isEqualTo(UNIT_PRICE);
        assertThat(response.totalPrice()).isEqualTo(UNIT_PRICE * 2);
        assertThat(response.productImageUrl()).isEqualTo(PRODUCT_IMAGE_URL);
        assertThat(response.quantity()).isEqualTo(order.getQuantity());
        assertThat(response.orderDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(response.message()).isEqualTo(order.getMessage());
    }

    private Order order() {
        return new Order(
            PRODUCT_ID,
            OPTION_ID,
            MEMBER_ID,
            PRODUCT_NAME,
            OPTION_NAME,
            UNIT_PRICE,
            PRODUCT_IMAGE_URL,
            2,
            "선물 메시지"
        );
    }

    private Option option() {
        Product product = new Product(
            "테스트 상품",
            30000,
            "https://example.com/product.png",
            1L
        );
        return product.addOption("기본 옵션", 5);
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to assign id for contract test.", e);
        }
    }
}
