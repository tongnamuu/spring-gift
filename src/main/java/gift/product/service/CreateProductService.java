package gift.product.service;

import gift.category.CategoryRepository;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.product.repository.ProductRepository;
import gift.product.usecase.CreateProductUseCase;
import gift.product.validator.ProductNameValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public ProductResponse execute(ProductRequest request) {
        validateName(request.name());
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + request.categoryId());
        }
        return ProductResponse.from(productRepository.save(request.toEntity()));
    }

    private void validateName(String name) {
        List<String> errors = ProductNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
