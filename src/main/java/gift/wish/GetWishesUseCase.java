package gift.wish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetWishesUseCase {
    Page<WishResponse> execute(Long memberId, Pageable pageable);
}
