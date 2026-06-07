package gift.order;

import gift.product.entity.Option;
import gift.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderContractTest {
    private static final Long MEMBER_ID = 1L;

    @Test
    void orderKeepsPurchasedOptionMemberQuantityMessageAndCreationTime() {
        Option option = option();
        LocalDateTime beforeCreation = LocalDateTime.now();

        Order order = new Order(option, MEMBER_ID, 2, "선물 메시지");

        assertThat(order.getOption()).isSameAs(option);
        assertThat(order.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getMessage()).isEqualTo("선물 메시지");
        assertThat(order.getOrderDateTime()).isAfterOrEqualTo(beforeCreation);
        assertThat(order.getOrderDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void orderCreationDoesNotChangeOptionStockByItself() {
        Option option = option();

        new Order(option, MEMBER_ID, 2, "선물 메시지");

        assertThat(option.getQuantity()).isEqualTo(5);
    }

    @Test
    void orderResponseRepresentsOrderEntity() {
        Option option = option();
        setId(option, 10L);
        Order order = new Order(option, MEMBER_ID, 2, "선물 메시지");
        setId(order, 20L);

        OrderResponse response = OrderResponse.from(order);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.optionId()).isEqualTo(10L);
        assertThat(response.quantity()).isEqualTo(order.getQuantity());
        assertThat(response.orderDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(response.message()).isEqualTo(order.getMessage());
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
