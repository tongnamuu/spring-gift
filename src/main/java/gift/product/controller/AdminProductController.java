package gift.product.controller;

import gift.category.controller.CategoryResponse;
import gift.product.dto.ProductCommand;
import gift.product.dto.ProductResponse;
import gift.product.usecase.CreateAdminProductUseCase;
import gift.product.usecase.DeleteAdminProductUseCase;
import gift.product.usecase.GetAdminProductUseCase;
import gift.product.usecase.GetAdminProductsUseCase;
import gift.product.usecase.GetProductFormCategoriesUseCase;
import gift.product.usecase.UpdateAdminProductUseCase;
import gift.product.vo.ProductName;
import gift.product.vo.ProductPrice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final GetAdminProductsUseCase getAdminProductsUseCase;
    private final GetAdminProductUseCase getAdminProductUseCase;
    private final CreateAdminProductUseCase createAdminProductUseCase;
    private final UpdateAdminProductUseCase updateAdminProductUseCase;
    private final DeleteAdminProductUseCase deleteAdminProductUseCase;
    private final GetProductFormCategoriesUseCase getProductFormCategoriesUseCase;

    public AdminProductController(
        GetAdminProductsUseCase getAdminProductsUseCase,
        GetAdminProductUseCase getAdminProductUseCase,
        CreateAdminProductUseCase createAdminProductUseCase,
        UpdateAdminProductUseCase updateAdminProductUseCase,
        DeleteAdminProductUseCase deleteAdminProductUseCase,
        GetProductFormCategoriesUseCase getProductFormCategoriesUseCase
    ) {
        this.getAdminProductsUseCase = getAdminProductsUseCase;
        this.getAdminProductUseCase = getAdminProductUseCase;
        this.createAdminProductUseCase = createAdminProductUseCase;
        this.updateAdminProductUseCase = updateAdminProductUseCase;
        this.deleteAdminProductUseCase = deleteAdminProductUseCase;
        this.getProductFormCategoriesUseCase = getProductFormCategoriesUseCase;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", getAdminProductsUseCase.execute());
        model.addAttribute("categoryNames", categoryNames());
        return "product/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categories", getProductFormCategoriesUseCase.execute());
        return "product/new";
    }

    @PostMapping
    public String create(
        @RequestParam String name,
        @RequestParam int price,
        @RequestParam String imageUrl,
        @RequestParam Long categoryId,
        Model model
    ) {
        ProductInputResult productInputResult = productInputAllowingKakao(name, price);
        if (productInputResult.hasErrors()) {
            populateNewForm(model, productInputResult.errors(), name, price, imageUrl, categoryId);
            return "product/new";
        }

        createAdminProductUseCase.execute(toCommand(productInputResult.name(), productInputResult.price(), imageUrl, categoryId));
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductResponse product = getAdminProductUseCase.execute(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", getProductFormCategoriesUseCase.execute());
        return "product/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable Long id,
        @RequestParam String name,
        @RequestParam int price,
        @RequestParam String imageUrl,
        @RequestParam Long categoryId,
        Model model
    ) {
        ProductResponse product = getAdminProductUseCase.execute(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));

        ProductInputResult productInputResult = productInputAllowingKakao(name, price);
        if (productInputResult.hasErrors()) {
            populateEditForm(model, product, productInputResult.errors(), name, price, imageUrl, categoryId);
            return "product/edit";
        }

        updateAdminProductUseCase.execute(id, toCommand(productInputResult.name(), productInputResult.price(), imageUrl, categoryId));
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        deleteAdminProductUseCase.execute(id);
        return "redirect:/admin/products";
    }

    private void populateNewForm(
        Model model,
        List<String> errors,
        String name,
        int price,
        String imageUrl,
        Long categoryId
    ) {
        model.addAttribute("errors", errors);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", getProductFormCategoriesUseCase.execute());
    }

    private void populateEditForm(
        Model model,
        ProductResponse product,
        List<String> errors,
        String name,
        int price,
        String imageUrl,
        Long categoryId
    ) {
        model.addAttribute("errors", errors);
        model.addAttribute("product", product);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", getProductFormCategoriesUseCase.execute());
    }

    private Map<Long, String> categoryNames() {
        return getProductFormCategoriesUseCase.execute().stream()
            .collect(Collectors.toMap(CategoryResponse::id, CategoryResponse::name));
    }

    private ProductCommand toCommand(ProductName name, ProductPrice price, String imageUrl, Long categoryId) {
        return new ProductCommand(name, price, imageUrl, categoryId);
    }

    private ProductInputResult productInputAllowingKakao(String name, int price) {
        List<String> errors = new ArrayList<>();
        ProductName productName = null;
        ProductPrice productPrice = null;

        try {
            productName = ProductName.allowingKakao(name);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }

        try {
            productPrice = new ProductPrice(price);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }

        return new ProductInputResult(productName, productPrice, errors);
    }

    private record ProductInputResult(ProductName name, ProductPrice price, List<String> errors) {
        private boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
