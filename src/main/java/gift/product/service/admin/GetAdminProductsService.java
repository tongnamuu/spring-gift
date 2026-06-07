package gift.product.service.admin;

import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.GetAdminProductsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetAdminProductsService implements GetAdminProductsUseCase {
    private final ProductRepository productRepository;

    public GetAdminProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> execute() {
        return productRepository.findAll();
    }
}
