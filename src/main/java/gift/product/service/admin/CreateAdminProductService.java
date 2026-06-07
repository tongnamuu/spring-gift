package gift.product.service.admin;

import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductCommand;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.CreateAdminProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CreateAdminProductService implements CreateAdminProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CreateAdminProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Product execute(ProductCommand command) {
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + command.categoryId());
        }
        return productRepository.save(command.toEntity());
    }
}
