package gift.order.usecase;

import gift.order.controller.OrderRequest;
import gift.order.controller.OrderResponse;

public interface CreateOrderUseCase {
    OrderResponse execute(Long memberId, OrderRequest request);
}
