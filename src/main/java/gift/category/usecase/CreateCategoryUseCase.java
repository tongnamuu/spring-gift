package gift.category.usecase;

import gift.category.controller.CategoryResponse;

public interface CreateCategoryUseCase {
    CategoryResponse execute(CategoryCommand command);
}
