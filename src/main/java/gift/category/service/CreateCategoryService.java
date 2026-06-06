package gift.category.service;

import gift.category.controller.CategoryRequest;
import gift.category.controller.CategoryResponse;
import gift.category.domain.Category;
import gift.category.domain.CategoryRepository;
import gift.category.usecase.CreateCategoryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCategoryService implements CreateCategoryUseCase {
    private final CategoryRepository categoryRepository;

    public CreateCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryResponse execute(CategoryRequest request) {
        Category saved = categoryRepository.save(request.toEntity());
        return CategoryResponse.from(saved);
    }
}
