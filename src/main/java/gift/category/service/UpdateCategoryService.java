package gift.category.service;

import gift.category.controller.CategoryRequest;
import gift.category.controller.CategoryResponse;
import gift.category.domain.CategoryRepository;
import gift.category.usecase.UpdateCategoryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateCategoryService implements UpdateCategoryUseCase {
    private final CategoryRepository categoryRepository;

    public UpdateCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Optional<CategoryResponse> execute(Long id, CategoryRequest request) {
        return categoryRepository.findById(id)
            .map(category -> {
                category.update(request.name(), request.color(), request.imageUrl(), request.description());
                return CategoryResponse.from(categoryRepository.save(category));
            });
    }
}
