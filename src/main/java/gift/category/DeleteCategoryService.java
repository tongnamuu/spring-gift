package gift.category;

import gift.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCategoryService implements DeleteCategoryUseCase {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DeleteCategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("상품이 있는 카테고리는 삭제할 수 없습니다.");
        }
        categoryRepository.deleteById(id);
    }
}
