package gift.category.usecase;

import gift.category.controller.CategoryResponse;
import gift.category.dto.CategoryCommand;

import java.util.Optional;

public interface UpdateCategoryUseCase {
    Optional<CategoryResponse> execute(Long id, CategoryCommand command);
}
