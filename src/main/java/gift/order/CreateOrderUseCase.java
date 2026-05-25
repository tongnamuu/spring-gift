package gift.order;

public interface CreateOrderUseCase {
    OrderResponse execute(Long memberId, OrderRequest request);
}
