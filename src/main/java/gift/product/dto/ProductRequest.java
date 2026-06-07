package gift.product.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
    @NotBlank String name,
    @NotNull Integer price,
    @NotBlank String imageUrl,
    @NotNull Long categoryId
) {
}
