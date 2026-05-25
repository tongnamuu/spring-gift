package gift.product;

public interface CreateAdminProductUseCase {
    Product execute(String name, int price, String imageUrl, Long categoryId);
}
