package gift.product.controller;

import gift.product.dto.ProductRequest;
import gift.product.dto.ProductResponse;
import gift.product.usecase.CreateProductUseCase;
import gift.product.dto.ProductCommand;
import gift.product.usecase.UpdateProductUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductControllerValidationTest {
    @Test
    void createProductDoesNotCallUseCaseWhenProductNameIsInvalid() {
        RecordingCreateProductUseCase createProductUseCase = new RecordingCreateProductUseCase();
        ProductController controller = controller(createProductUseCase, new RecordingUpdateProductUseCase());
        ProductRequest request = new ProductRequest(
            "카카오상품",
            1000,
            "https://example.com/product.png",
            1L
        );

        assertThatThrownBy(() -> controller.createProduct(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");

        assertThat(createProductUseCase.called()).isFalse();
    }

    @Test
    void updateProductDoesNotCallUseCaseWhenProductNameIsInvalid() {
        RecordingUpdateProductUseCase updateProductUseCase = new RecordingUpdateProductUseCase();
        ProductController controller = controller(new RecordingCreateProductUseCase(), updateProductUseCase);
        ProductRequest request = new ProductRequest(
            "카카오상품",
            1000,
            "https://example.com/product.png",
            1L
        );

        assertThatThrownBy(() -> controller.updateProduct(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");

        assertThat(updateProductUseCase.called()).isFalse();
    }

    @Test
    void createProductDoesNotCallUseCaseWhenProductPriceIsZero() {
        RecordingCreateProductUseCase createProductUseCase = new RecordingCreateProductUseCase();
        ProductController controller = controller(createProductUseCase, new RecordingUpdateProductUseCase());
        ProductRequest request = new ProductRequest(
            "상품",
            0,
            "https://example.com/product.png",
            1L
        );

        assertThatThrownBy(() -> controller.createProduct(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 가격은 1 이상이어야 합니다.");

        assertThat(createProductUseCase.called()).isFalse();
    }

    @Test
    void updateProductDoesNotCallUseCaseWhenProductPriceIsZero() {
        RecordingUpdateProductUseCase updateProductUseCase = new RecordingUpdateProductUseCase();
        ProductController controller = controller(new RecordingCreateProductUseCase(), updateProductUseCase);
        ProductRequest request = new ProductRequest(
            "상품",
            0,
            "https://example.com/product.png",
            1L
        );

        assertThatThrownBy(() -> controller.updateProduct(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 가격은 1 이상이어야 합니다.");

        assertThat(updateProductUseCase.called()).isFalse();
    }

    private ProductController controller(
        RecordingCreateProductUseCase createProductUseCase,
        RecordingUpdateProductUseCase updateProductUseCase
    ) {
        return new ProductController(
            pageable -> Page.empty(),
            id -> Optional.empty(),
            createProductUseCase,
            updateProductUseCase,
            id -> {
            }
        );
    }

    private static class RecordingCreateProductUseCase implements CreateProductUseCase {
        private boolean called;

        @Override
        public ProductResponse execute(ProductCommand command) {
            called = true;
            return new ProductResponse(
                1L,
                command.name().value(),
                command.price().value(),
                command.imageUrl(),
                command.categoryId(),
                List.of()
            );
        }

        private boolean called() {
            return called;
        }
    }

    private static class RecordingUpdateProductUseCase implements UpdateProductUseCase {
        private boolean called;

        @Override
        public Optional<ProductResponse> execute(Long id, ProductCommand command) {
            called = true;
            return Optional.of(new ProductResponse(
                id,
                command.name().value(),
                command.price().value(),
                command.imageUrl(),
                command.categoryId(),
                List.of()
            ));
        }

        private boolean called() {
            return called;
        }
    }
}
