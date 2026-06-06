package gift.product.service;

import gift.category.CategoryRepository;
import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.product.repository.ProductRepository;
import gift.product.usecase.UpdateProductUseCase;
import gift.product.validator.ProductNameValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UpdateProductService implements UpdateProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public UpdateProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Optional<ProductResponse> execute(Long id, ProductRequest request) {
        validateName(request.name());
        if (!categoryRepository.existsById(request.categoryId())) {
            return Optional.empty();
        }
        return productRepository.findById(id)
            .map(product -> {
                product.update(request.name(), request.price(), request.imageUrl(), request.categoryId());
                return ProductResponse.from(productRepository.save(product));
            });
    }

    private void validateName(String name) {
        List<String> errors = ProductNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
