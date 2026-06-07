package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CreateOrderService implements CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateOrderService(
        OrderRepository orderRepository,
        ProductRepository productRepository,
        MemberRepository memberRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OrderResponse execute(Long memberId, OrderRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + memberId));
        Product product = productRepository.findByOptionId(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + request.optionId()));

        Option option = product.subtractOptionQuantity(request.optionId(), request.quantity());

        int price = product.getPrice() * request.quantity();
        member.deductPoint(price);

        Order saved = orderRepository.save(new Order(
            product.getId(),
            option.getId(),
            member.getId(),
            product.getName(),
            option.getName(),
            product.getPrice(),
            product.getImageUrl(),
            request.quantity(),
            request.message()
        ));
        publishKakaoMessageEvent(member.getKakaoAccessToken(), saved);
        return OrderResponse.from(saved);
    }

    private void publishKakaoMessageEvent(String accessToken, Order order) {
        if (accessToken == null) {
            return;
        }
        eventPublisher.publishEvent(new OrderCreatedEvent(accessToken, KakaoOrderMessage.from(order)));
    }
}
