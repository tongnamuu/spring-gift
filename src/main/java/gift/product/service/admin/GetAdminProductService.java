package gift.product.service.admin;

import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.GetAdminProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetAdminProductService implements GetAdminProductUseCase {
    private final ProductRepository productRepository;

    public GetAdminProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> execute(Long id) {
        return productRepository.findById(id);
    }
}
