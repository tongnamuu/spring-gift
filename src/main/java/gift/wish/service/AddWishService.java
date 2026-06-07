package gift.wish.service;

import gift.product.repository.ProductRepository;
import gift.wish.controller.WishResponse;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import gift.wish.usecase.AddWishResult;
import gift.wish.usecase.AddWishUseCase;
import gift.wish.dto.WishCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class AddWishService implements AddWishUseCase {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public AddWishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public AddWishResult execute(Long memberId, WishCommand command) {
        var product = productRepository.findById(command.productId())
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + command.productId()));

        return wishRepository.findByMemberIdAndProductId(memberId, product.getId())
            .map(existing -> new AddWishResult(
                toResponse(existing, product.getName(), product.getPrice(), product.getImageUrl()),
                false
            ))
            .orElseGet(() -> {
                Wish saved = wishRepository.save(new Wish(memberId, product.getId()));
                return new AddWishResult(
                    toResponse(saved, product.getName(), product.getPrice(), product.getImageUrl()),
                    true
                );
            });
    }

    private WishResponse toResponse(Wish wish, String productName, int productPrice, String productImageUrl) {
        return WishResponse.from(wish, productName, productPrice, productImageUrl);
    }
}
