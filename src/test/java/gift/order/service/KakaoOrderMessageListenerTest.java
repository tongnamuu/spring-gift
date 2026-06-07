package gift.order.service;

import gift.order.domain.Order;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoOrderMessageListenerTest {
    @Test
    void kakaoOrderMessageListenerRunsAsAfterCommitAsyncEventHandler() throws NoSuchMethodException {
        Method handle = KakaoOrderMessageListener.class.getDeclaredMethod("handle", OrderCreatedEvent.class);

        assertThat(handle.isAnnotationPresent(Async.class)).isTrue();
        assertThat(handle.getAnnotation(TransactionalEventListener.class).phase())
            .isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void kakaoOrderMessageUsesOrderSnapshot() {
        Order order = new Order(
            1L,
            2L,
            3L,
            "2025 햅쌀",
            "2025년 재배",
            30000,
            "https://example.com/rice.png",
            2,
            "선물 메시지"
        );

        KakaoOrderMessage message = KakaoOrderMessage.from(order);

        assertThat(message.productName()).isEqualTo("2025 햅쌀");
        assertThat(message.optionName()).isEqualTo("2025년 재배");
        assertThat(message.unitPrice()).isEqualTo(30000);
        assertThat(message.quantity()).isEqualTo(2);
        assertThat(message.message()).isEqualTo("선물 메시지");
    }
}
