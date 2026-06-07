package gift.product.service;

import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductResponse;
import gift.product.repository.ProductRepository;
import gift.product.usecase.ProductCommand;
import gift.product.usecase.UpdateProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<ProductResponse> execute(Long id, ProductCommand command) {
        return productRepository.findById(id)
            .map(product -> {
                if (!categoryRepository.existsById(command.categoryId())) {
                    throw new IllegalArgumentException("카테고리가 존재하지 않습니다. id=" + command.categoryId());
                }
                product.update(command.name(), command.price(), command.imageUrl(), command.categoryId());
                return ProductResponse.from(productRepository.save(product));
            });
    }
}
