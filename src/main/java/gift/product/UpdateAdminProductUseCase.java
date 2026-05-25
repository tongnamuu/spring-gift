package gift.product;

public interface UpdateAdminProductUseCase {
    Product execute(Long id, String name, int price, String imageUrl, Long categoryId);
}
