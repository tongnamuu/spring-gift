package gift.category.service;

import gift.category.domain.CategoryRepository;
import gift.category.usecase.DeleteCategoryUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCategoryService implements DeleteCategoryUseCase {
    private final CategoryRepository categoryRepository;

    public DeleteCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        categoryRepository.deleteById(id);
    }
}
