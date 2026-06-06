package gift.category.service;

import gift.category.controller.CategoryResponse;
import gift.category.domain.CategoryRepository;
import gift.category.usecase.GetCategoriesUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetCategoriesService implements GetCategoriesUseCase {
    private final CategoryRepository categoryRepository;

    public GetCategoriesService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> execute() {
        return categoryRepository.findAll().stream()
            .map(CategoryResponse::from)
            .toList();
    }
}
