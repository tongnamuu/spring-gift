package gift.product.query;

import gift.product.dto.ProductResponse;
import gift.product.usecase.GetProductsUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProductsService implements GetProductsUseCase {
    private final ProductQueryDao productQueryDao;

    public GetProductsService(ProductQueryDao productQueryDao) {
        this.productQueryDao = productQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> execute(Pageable pageable) {
        return productQueryDao.findResponses(pageable);
    }
}
