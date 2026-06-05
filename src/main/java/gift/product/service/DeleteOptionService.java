package gift.product.service;

import gift.product.entity.Option;
import gift.product.entity.Product;
import gift.product.repository.OptionRepository;
import gift.product.repository.ProductRepository;
import gift.product.usecase.DeleteOptionUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class DeleteOptionService implements DeleteOptionUseCase {
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;

    public DeleteOptionService(ProductRepository productRepository, OptionRepository optionRepository) {
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional
    public void execute(Long productId, Long optionId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + productId));
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. id=" + optionId));
        product.removeOption(option);
        productRepository.save(product);
    }
}
