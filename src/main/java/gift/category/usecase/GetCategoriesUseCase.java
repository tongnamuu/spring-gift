package gift.category.usecase;

import gift.category.controller.CategoryResponse;

import java.util.List;

public interface GetCategoriesUseCase {
    List<CategoryResponse> execute();
}
