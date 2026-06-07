package gift.category.controller;

import gift.category.usecase.CategoryCommand;
import gift.category.usecase.CreateCategoryUseCase;
import gift.category.usecase.DeleteCategoryUseCase;
import gift.category.usecase.GetCategoriesUseCase;
import gift.category.usecase.UpdateCategoryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryController(
        GetCategoriesUseCase getCategoriesUseCase,
        CreateCategoryUseCase createCategoryUseCase,
        UpdateCategoryUseCase updateCategoryUseCase,
        DeleteCategoryUseCase deleteCategoryUseCase
    ) {
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(getCategoriesUseCase.execute());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = createCategoryUseCase.execute(toCommand(request));
        return ResponseEntity.created(URI.create("/api/categories/" + response.id()))
            .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody CategoryRequest request
    ) {
        return updateCategoryUseCase.execute(id, toCommand(request))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private CategoryCommand toCommand(CategoryRequest request) {
        return new CategoryCommand(
            request.name(),
            request.color(),
            request.imageUrl(),
            request.description()
        );
    }
}
