package gift.product.service;

import gift.product.dto.OptionResponse;
import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import gift.product.usecase.OptionCommand;
import gift.product.usecase.CreateOptionUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class CreateOptionService implements CreateOptionUseCase {
    private final ProductRepository productRepository;

    public CreateOptionService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public OptionResponse execute(Long productId, OptionCommand command) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        Option option = product.addOption(command.name(), command.quantity());
        productRepository.save(product);
        Option savedOption = productRepository.findOptionByProductIdAndName(productId, command.name().value())
            .orElse(option);
        return OptionResponse.from(savedOption);
    }
}
