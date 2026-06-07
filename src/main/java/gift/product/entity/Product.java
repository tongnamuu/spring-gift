package gift.product.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
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

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    @Column(name = "update_dt", nullable = false)
    private LocalDateTime updateDt;

    @Version
    private Long version;

    protected Product() {
    }

    public Product(String name, int price, String imageUrl, Long categoryId) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.updateDt = LocalDateTime.now();
    }

    public Product(ProductName name, int price, String imageUrl, Long categoryId) {
        this(name.value(), price, imageUrl, categoryId);
    }

    @PrePersist
    void prePersist() {
        if (updateDt == null) {
            updateDt = LocalDateTime.now();
        }
    }

    public void update(String name, int price, String imageUrl, Long categoryId) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        recordUpdated();
    }

    public void update(ProductName name, int price, String imageUrl, Long categoryId) {
        update(name.value(), price, imageUrl, categoryId);
    }

    public Option addOption(String name, int quantity) {
        if (hasOptionName(name)) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option option = new Option(this, name, quantity);
        options.add(option);
        recordUpdated();
        return option;
    }

    public Option addOption(OptionName name, int quantity) {
        return addOption(name.value(), quantity);
    }

    public void removeOption(Option option) {
        if (!hasOption(option)) {
            throw new IllegalArgumentException("상품에 속하지 않은 옵션입니다.");
        }
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        options.removeIf(existing -> isSameOption(existing, option));
        recordUpdated();
    }

    public void removeOption(Long optionId) {
        Option option = findOption(optionId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        options.remove(option);
        recordUpdated();
    }

    public Option subtractOptionQuantity(Long optionId, int amount) {
        Option option = findOption(optionId);
        option.subtractQuantity(amount);
        recordUpdated();
        return option;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getUpdateDt() {
        return updateDt;
    }

    private Option findOption(Long optionId) {
        return options.stream()
            .filter(option -> option.getId() != null && option.getId().equals(optionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("상품에 속하지 않은 옵션입니다."));
    }

    private void recordUpdated() {
        updateDt = LocalDateTime.now();
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
