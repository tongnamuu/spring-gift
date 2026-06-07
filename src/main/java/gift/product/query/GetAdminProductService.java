package gift.product.query;

import gift.product.dto.ProductResponse;
import gift.product.usecase.GetAdminProductUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetAdminProductService implements GetAdminProductUseCase {
    private final ProductQueryDao productQueryDao;

    public GetAdminProductService(ProductQueryDao productQueryDao) {
        this.productQueryDao = productQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductResponse> execute(Long id) {
        return productQueryDao.findResponseById(id);
    }
}
