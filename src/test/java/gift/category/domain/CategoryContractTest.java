package gift.category.domain;

import gift.category.controller.CategoryRequest;
import gift.category.controller.CategoryResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryContractTest {
    @Test
    void categoryCanExistWithoutProduct() {
        Category category = new Category(
            "테스트 카테고리",
            "#123456",
            "https://example.com/category.png",
            "상품 없이도 존재하는 카테고리"
        );

        assertThat(category.getName()).isEqualTo("테스트 카테고리");
        assertThat(category.getColor()).isEqualTo("#123456");
        assertThat(category.getImageUrl()).isEqualTo("https://example.com/category.png");
        assertThat(category.getDescription()).isEqualTo("상품 없이도 존재하는 카테고리");
    }

    @Test
    void updateChangesCategoryAttributes() {
        Category category = new Category(
            "기존 카테고리",
            "#111111",
            "https://example.com/old.png",
            "old description"
        );

        category.update(
            "변경 카테고리",
            "#222222",
            "https://example.com/new.png",
            "new description"
        );

        assertThat(category.getName()).isEqualTo("변경 카테고리");
        assertThat(category.getColor()).isEqualTo("#222222");
        assertThat(category.getImageUrl()).isEqualTo("https://example.com/new.png");
        assertThat(category.getDescription()).isEqualTo("new description");
    }

    @Test
    void categoryRequestCreatesCategoryEntity() {
        CategoryRequest request = new CategoryRequest(
            "요청 카테고리",
            "#333333",
            "https://example.com/request.png",
            "request description"
        );

        Category category = request.toEntity();

        assertThat(category.getName()).isEqualTo(request.name());
        assertThat(category.getColor()).isEqualTo(request.color());
        assertThat(category.getImageUrl()).isEqualTo(request.imageUrl());
        assertThat(category.getDescription()).isEqualTo(request.description());
    }

    @Test
    void categoryResponseRepresentsCategoryEntity() {
        Category category = new Category(
            "응답 카테고리",
            "#444444",
            "https://example.com/response.png",
            "response description"
        );
        setId(category, 1L);

        CategoryResponse response = CategoryResponse.from(category);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(category.getName());
        assertThat(response.color()).isEqualTo(category.getColor());
        assertThat(response.imageUrl()).isEqualTo(category.getImageUrl());
        assertThat(response.description()).isEqualTo(category.getDescription());
    }

    private void setId(Category category, Long id) {
        try {
            Field field = Category.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(category, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to assign category id for contract test.", e);
        }
    }
}
