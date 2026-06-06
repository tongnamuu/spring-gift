package gift.product.usecase;

import gift.category.domain.Category;

import java.util.List;

public interface GetProductFormCategoriesUseCase {
    List<Category> execute();
}
