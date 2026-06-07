package gift.order.usecase;

import gift.order.controller.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetOrdersUseCase {
    Page<OrderResponse> execute(Long memberId, Pageable pageable);
}
