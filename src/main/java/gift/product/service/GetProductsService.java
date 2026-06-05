package gift.product.service;

import gift.product.dto.ProductResponse;
import gift.product.repository.ProductRepository;
import gift.product.usecase.GetProductsUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProductsService implements GetProductsUseCase {
    private final ProductRepository productRepository;

    public GetProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> execute(Pageable pageable) {
        return productRepository.findAll(pageable)
            .map(ProductResponse::from);
    }
}
