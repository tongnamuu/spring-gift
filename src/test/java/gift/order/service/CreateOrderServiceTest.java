package gift.order.service;

import gift.member.domain.Member;
import gift.member.domain.MemberRepository;
import gift.order.domain.Order;
import gift.order.domain.OrderRepository;
import gift.order.usecase.OrderCommand;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateOrderServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 2L;
    private static final Long OPTION_ID = 3L;

    @Test
    void publishesOrderCreatedEventWhenOrderCreationSucceeds() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CreateOrderService service = new CreateOrderService(
            orderRepository,
            productRepository,
            memberRepository,
            eventPublisher
        );
        Member member = kakaoMember(100000);
        Product product = product();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(productRepository.findByOptionId(OPTION_ID)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(MEMBER_ID, new OrderCommand(OPTION_ID, 2, "성공 메시지"));

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        OrderCreatedEvent event = eventCaptor.getValue();
        assertThat(event.kakaoAccessToken()).isEqualTo("kakao-access-token");
        assertThat(event.message().productName()).isEqualTo("2025 햅쌀");
        assertThat(event.message().optionName()).isEqualTo("2025년 재배");
        assertThat(event.message().unitPrice()).isEqualTo(30000);
        assertThat(event.message().quantity()).isEqualTo(2);
        assertThat(event.message().message()).isEqualTo("성공 메시지");
    }

    @Test
    void doesNotPublishOrderCreatedEventWhenOrderCreationFails() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CreateOrderService service = new CreateOrderService(
            orderRepository,
            productRepository,
            memberRepository,
            eventPublisher
        );
        Member member = kakaoMember(1000);
        Product product = product();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(productRepository.findByOptionId(OPTION_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.execute(MEMBER_ID, new OrderCommand(OPTION_ID, 1, "실패 메시지")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("포인트가 부족합니다.");

        verify(eventPublisher, never()).publishEvent(any(OrderCreatedEvent.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Member kakaoMember(int point) {
        Member member = new Member("order-event@example.com", "password123");
        setId(member, MEMBER_ID);
        member.chargePoint(point);
        member.updateKakaoAccessToken("kakao-access-token");
        return member;
    }

    private Product product() {
        Product product = new Product(
            "2025 햅쌀",
            30000,
            "https://example.com/rice.png",
            10L
        );
        setId(product, PRODUCT_ID);
        Option option = product.addOption("2025년 재배", 10);
        setId(option, OPTION_ID);
        return product;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to assign id for test.", e);
        }
    }
}
