package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.NoSuchElementException;

@Service
public class CreateOrderService implements CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public CreateOrderService(
        OrderRepository orderRepository,
        ProductRepository productRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
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
        sendKakaoMessageAfterCommit(member.getKakaoAccessToken(), saved);
        return OrderResponse.from(saved);
    }

    private void sendKakaoMessageAfterCommit(String accessToken, Order order) {
        if (accessToken == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendKakaoMessageIfPossible(accessToken, order);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendKakaoMessageIfPossible(accessToken, order);
            }
        });
    }

    private void sendKakaoMessageIfPossible(String accessToken, Order order) {
        try {
            kakaoMessageClient.sendToMe(accessToken, order);
        } catch (Exception ignored) {
        }
    }
}
