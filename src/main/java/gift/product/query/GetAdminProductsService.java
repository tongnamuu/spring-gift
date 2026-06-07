package gift.product.query;

import gift.product.dto.ProductResponse;
import gift.product.usecase.GetAdminProductsUseCase;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetAdminProductsService implements GetAdminProductsUseCase {
    private final ProductQueryDao productQueryDao;

    public GetAdminProductsService(ProductQueryDao productQueryDao) {
        this.productQueryDao = productQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> execute() {
        return productQueryDao.findResponses(Pageable.unpaged()).getContent();
    }
}
