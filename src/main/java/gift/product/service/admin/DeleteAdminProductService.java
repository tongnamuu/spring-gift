package gift.product.service.admin;

import gift.product.repository.ProductRepository;
import gift.product.usecase.DeleteAdminProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteAdminProductService implements DeleteAdminProductUseCase {
    private final ProductRepository productRepository;

    public DeleteAdminProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        productRepository.deleteById(id);
    }
}
