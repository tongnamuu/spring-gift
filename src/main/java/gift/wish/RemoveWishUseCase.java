package gift.wish;

public interface RemoveWishUseCase {
    void execute(Long memberId, Long wishId);
}
