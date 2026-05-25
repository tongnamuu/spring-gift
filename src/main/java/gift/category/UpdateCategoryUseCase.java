package gift.category;

import java.util.Optional;

public interface UpdateCategoryUseCase {
    Optional<CategoryResponse> execute(Long id, CategoryRequest request);
}
