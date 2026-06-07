package gift.category.usecase;

import gift.category.controller.CategoryResponse;
import gift.category.dto.CategoryCommand;

public interface CreateCategoryUseCase {
    CategoryResponse execute(CategoryCommand command);
}
