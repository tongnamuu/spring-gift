package gift.product.service;

import gift.product.repository.ProductRepository;
import gift.product.usecase.DeleteProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProductService implements DeleteProductUseCase {
    private final ProductRepository productRepository;

    public DeleteProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        productRepository.deleteById(id);
    }
}
