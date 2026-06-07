package gift.order.usecase;

import gift.order.controller.OrderResponse;
import gift.order.dto.OrderCommand;

public interface CreateOrderUseCase {
    OrderResponse execute(Long memberId, OrderCommand command);
}
