package gift.wish.controller;

import jakarta.validation.constraints.NotNull;

public record WishRequest(@NotNull Long productId) {
}
