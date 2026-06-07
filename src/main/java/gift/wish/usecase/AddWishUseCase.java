package gift.wish.usecase;

import gift.wish.dto.WishCommand;

public interface AddWishUseCase {
    AddWishResult execute(Long memberId, WishCommand command);
}
