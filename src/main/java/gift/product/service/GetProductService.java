package gift.product.service;

import gift.product.dto.ProductResponse;
import gift.product.repository.ProductRepository;
import gift.product.usecase.GetProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetProductService implements GetProductUseCase {
    private final ProductRepository productRepository;

    public GetProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductResponse> execute(Long id) {
        return productRepository.findById(id)
            .map(ProductResponse::from);
    }
}
