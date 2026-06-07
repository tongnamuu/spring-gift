package gift.product.service;

import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductResponse;
import gift.product.repository.ProductRepository;
import gift.product.dto.ProductCommand;
import gift.product.usecase.CreateProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CreateProductService implements CreateProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CreateProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ProductResponse execute(ProductCommand command) {
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + command.categoryId());
        }
        return ProductResponse.from(productRepository.save(command.toEntity()));
    }
}
