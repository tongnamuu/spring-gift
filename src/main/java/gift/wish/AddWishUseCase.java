package gift.wish;

public interface AddWishUseCase {
    WishResponse execute(Long memberId, WishRequest request);
}
