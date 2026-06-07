package gift.category.dto;

import gift.category.domain.Category;

public record CategoryCommand(
    String name,
    String color,
    String imageUrl,
    String description
) {
    public Category toEntity() {
        return new Category(name, color, imageUrl, description);
    }
}
