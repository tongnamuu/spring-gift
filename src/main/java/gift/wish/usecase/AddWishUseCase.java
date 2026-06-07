package gift.wish.usecase;

public interface AddWishUseCase {
    AddWishResult execute(Long memberId, WishCommand command);
}
