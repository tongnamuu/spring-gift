package gift.wish.usecase;

import gift.wish.controller.WishResponse;

public record AddWishResult(WishResponse response, boolean created) {
}

