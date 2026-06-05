package gift.product.service;

import gift.product.dto.OptionRequest;
import gift.product.dto.OptionResponse;
import gift.product.entity.Product;
import gift.product.repository.OptionRepository;
import gift.product.repository.ProductRepository;
import gift.product.usecase.CreateOptionUseCase;
import gift.product.validator.OptionNameValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CreateOptionService implements CreateOptionUseCase {
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public CreateOptionService(ProductRepository productRepository, OptionRepository optionRepository) {
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional
    public OptionResponse execute(Long productId, OptionRequest request) {
        validateName(request.name());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        return OptionResponse.from(optionRepository.save(product.addOption(request.name(), request.quantity())));
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
