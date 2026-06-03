package gift.product.usecase;

import gift.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetProductsUseCase {
    Page<ProductResponse> execute(Pageable pageable);
}
