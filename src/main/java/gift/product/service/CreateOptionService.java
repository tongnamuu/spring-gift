package gift.product.service;

import gift.product.dto.OptionRequest;
import gift.product.dto.OptionResponse;
import gift.product.entity.Option;
import gift.product.entity.Product;
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

    public CreateOptionService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public OptionResponse execute(Long productId, OptionRequest request) {
        validateName(request.name());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        Option option = product.addOption(request.name(), request.quantity());
        productRepository.save(product);
        Option savedOption = productRepository.findOptionByProductIdAndName(productId, request.name())
            .orElse(option);
        return OptionResponse.from(savedOption);
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
