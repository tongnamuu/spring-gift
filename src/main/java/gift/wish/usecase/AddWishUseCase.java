package gift.wish.usecase;

import gift.wish.controller.WishRequest;

public interface AddWishUseCase {
    AddWishResult execute(Long memberId, WishRequest request);
}
