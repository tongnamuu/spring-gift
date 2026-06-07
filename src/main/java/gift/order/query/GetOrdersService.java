package gift.order.query;

import gift.order.controller.OrderResponse;
import gift.order.usecase.GetOrdersUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrdersService implements GetOrdersUseCase {
    private final OrderQueryDao orderQueryDao;

    public GetOrdersService(OrderQueryDao orderQueryDao) {
        this.orderQueryDao = orderQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> execute(Long memberId, Pageable pageable) {
        return orderQueryDao.findResponsesByMemberId(memberId, pageable);
    }
}
