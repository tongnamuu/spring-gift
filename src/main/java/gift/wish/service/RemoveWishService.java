package gift.wish.service;

import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import gift.wish.usecase.RemoveWishUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class RemoveWishService implements RemoveWishUseCase {
    private final WishRepository wishRepository;

    public RemoveWishService(WishRepository wishRepository) {
        this.wishRepository = wishRepository;
    }

    @Override
    @Transactional
    public void execute(Long memberId, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));

        if (!wish.isOwnedBy(memberId)) {
            throw new WishAccessDeniedException();
        }

        wishRepository.delete(wish);
    }
}

