package gift.product.service.admin;

import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.product.usecase.GetProductFormCategoriesUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetProductFormCategoriesService implements GetProductFormCategoriesUseCase {
    private final CategoryRepository categoryRepository;

    public GetProductFormCategoriesService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> execute() {
        return categoryRepository.findAll();
    }
}
