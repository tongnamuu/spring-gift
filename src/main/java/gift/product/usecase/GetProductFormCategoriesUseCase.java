package gift.product.usecase;

import gift.category.controller.CategoryResponse;

import java.util.List;

public interface GetProductFormCategoriesUseCase {
    List<CategoryResponse> execute();
}
