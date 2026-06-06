package gift.category.usecase;

import gift.category.controller.CategoryRequest;
import gift.category.controller.CategoryResponse;

import java.util.Optional;

public interface UpdateCategoryUseCase {
    Optional<CategoryResponse> execute(Long id, CategoryRequest request);
}
