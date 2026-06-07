package gift.wish.service;

import gift.wish.controller.WishResponse;
import gift.wish.usecase.GetWishesUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetWishesService implements GetWishesUseCase {
    private final WishQueryDao wishQueryDao;

    public GetWishesService(WishQueryDao wishQueryDao) {
        this.wishQueryDao = wishQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WishResponse> execute(Long memberId, Pageable pageable) {
        return wishQueryDao.findResponsesByMemberId(memberId, pageable);
    }
}
