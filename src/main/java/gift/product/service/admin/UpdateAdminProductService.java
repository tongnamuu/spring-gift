package gift.product.service.admin;

import gift.category.domain.CategoryRepository;
import gift.product.dto.ProductCommand;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.UpdateAdminProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class UpdateAdminProductService implements UpdateAdminProductUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public UpdateAdminProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Product execute(Long id, ProductCommand command) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + command.categoryId());
        }

        product.update(command.name(), command.price(), command.imageUrl(), command.categoryId());
        return productRepository.save(product);
    }
}
