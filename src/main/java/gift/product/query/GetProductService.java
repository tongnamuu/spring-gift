package gift.product.query;

import gift.product.dto.ProductResponse;
import gift.product.usecase.GetProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetProductService implements GetProductUseCase {
    private final ProductQueryDao productQueryDao;

    public GetProductService(ProductQueryDao productQueryDao) {
        this.productQueryDao = productQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductResponse> execute(Long id) {
        return productQueryDao.findResponseById(id);
    }
}
