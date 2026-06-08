package gift.wish.service;

import gift.product.repository.ProductRepository;
import gift.wish.controller.WishResponse;
import gift.wish.domain.Wish;
import gift.wish.domain.WishRepository;
import gift.wish.usecase.AddWishResult;
import gift.wish.usecase.AddWishUseCase;
import gift.wish.dto.WishCommand;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class AddWishService implements AddWishUseCase {
    private static final String DUPLICATE_WISH_MESSAGE = "이미 위시한 상품입니다.";

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

        try {
            Wish saved = wishRepository.save(new Wish(memberId, product.getId()));
            return new AddWishResult(
                toResponse(saved, product.getName(), product.getPrice(), product.getImageUrl()),
                true
            );
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateWishViolation(e)) {
                throw new IllegalArgumentException(DUPLICATE_WISH_MESSAGE, e);
            }
            throw e;
        }
    }

    private boolean isDuplicateWishViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_wish_member_product");
    }

    private WishResponse toResponse(Wish wish, String productName, int productPrice, String productImageUrl) {
        return WishResponse.from(wish, productName, productPrice, productImageUrl);
    }
}
