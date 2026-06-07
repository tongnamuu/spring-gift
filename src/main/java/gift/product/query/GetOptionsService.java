package gift.product.query;

import gift.product.dto.OptionResponse;
import gift.product.usecase.GetOptionsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GetOptionsService implements GetOptionsUseCase {
    private final ProductQueryDao productQueryDao;
    private final OptionQueryDao optionQueryDao;

    public GetOptionsService(ProductQueryDao productQueryDao, OptionQueryDao optionQueryDao) {
        this.productQueryDao = productQueryDao;
        this.optionQueryDao = optionQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<OptionResponse>> execute(Long productId) {
        if (!productQueryDao.existsById(productId)) {
            return Optional.empty();
        }
        return Optional.of(optionQueryDao.findResponsesByProductId(productId));
    }
}
