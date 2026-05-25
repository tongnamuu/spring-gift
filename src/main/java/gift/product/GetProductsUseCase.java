package gift.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetProductsUseCase {
    Page<ProductResponse> execute(Pageable pageable);
}
