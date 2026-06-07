package gift.product.query;

import gift.product.dto.OptionResponse;
import gift.product.repository.ProductRepository;
import gift.product.usecase.GetOptionsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GetOptionsService implements GetOptionsUseCase {
    private final ProductRepository productRepository;
    private final OptionQueryDao optionQueryDao;

    public GetOptionsService(ProductRepository productRepository, OptionQueryDao optionQueryDao) {
        this.productRepository = productRepository;
        this.optionQueryDao = optionQueryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<OptionResponse>> execute(Long productId) {
        if (!productRepository.existsById(productId)) {
            return Optional.empty();
        }
        return Optional.of(optionQueryDao.findResponsesByProductId(productId));
    }
}
