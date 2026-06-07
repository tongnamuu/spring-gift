package gift.product.query;

import gift.category.controller.CategoryResponse;
import gift.category.query.CategoryQueryDao;
import gift.product.usecase.GetProductFormCategoriesUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetProductFormCategoriesService implements GetProductFormCategoriesUseCase {
    private final CategoryQueryDao categoryQueryDao;

    public GetProductFormCategoriesService(CategoryQueryDao categoryQueryDao) {
        this.categoryQueryDao = categoryQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> execute() {
        return categoryQueryDao.findResponses();
    }
}
