package gift.category.service;

import gift.category.controller.CategoryResponse;
import gift.category.domain.CategoryRepository;
import gift.category.dto.CategoryCommand;
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
    public CategoryResponse execute(CategoryCommand command) {
        var saved = categoryRepository.save(command.toEntity());
        return CategoryResponse.from(saved);
    }
}
