package gift.order.usecase;

import gift.order.controller.OrderResponse;

public interface CreateOrderUseCase {
    OrderResponse execute(Long memberId, OrderCommand command);
}
