package gift.product.entity;

import gift.category.Category;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int price;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, int price, String imageUrl, Category category) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void update(String name, int price, String imageUrl, Category category) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public Option addOption(String name, int quantity) {
        if (hasOptionName(name)) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option option = new Option(this, name, quantity);
        options.add(option);
        return option;
    }

    public void removeOption(Option option) {
        if (!hasOption(option)) {
            throw new IllegalArgumentException("상품에 속하지 않은 옵션입니다.");
        }
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        options.removeIf(existing -> isSameOption(existing, option));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Category getCategory() {
        return category;
    }

    public List<Option> getOptions() {
        return options;
    }

    private boolean hasOptionName(String name) {
        return options.stream()
            .anyMatch(option -> option.getName().equals(name));
    }

    private boolean hasOption(Option option) {
        return options.stream()
            .anyMatch(existing -> isSameOption(existing, option));
    }

    private boolean isSameOption(Option left, Option right) {
        if (left == right) {
            return true;
        }
        if (left.getId() == null || right.getId() == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }
}
