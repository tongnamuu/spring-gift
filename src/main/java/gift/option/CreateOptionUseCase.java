package gift.option;

public interface CreateOptionUseCase {
    OptionResponse execute(Long productId, OptionRequest request);
}
