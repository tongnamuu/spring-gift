package gift.product.service;

import gift.product.dto.OptionResponse;
import gift.product.repository.OptionRepository;
import gift.product.repository.ProductRepository;
import gift.product.usecase.GetOptionsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GetOptionsService implements GetOptionsUseCase {
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public GetOptionsService(ProductRepository productRepository, OptionRepository optionRepository) {
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<OptionResponse>> execute(Long productId) {
        if (!productRepository.existsById(productId)) {
            return Optional.empty();
        }
        return Optional.of(optionRepository.findByProductId(productId).stream()
            .map(OptionResponse::from)
            .toList());
    }
}
