package gift.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "product_image_url", nullable = false)
    private String productImageUrl;

    private int quantity;

    private String message;

    private LocalDateTime orderDateTime;

    protected Order() {
    }

    public Order(
        Long productId,
        Long optionId,
        Long memberId,
        String productName,
        String optionName,
        int unitPrice,
        String productImageUrl,
        int quantity,
        String message
    ) {
        this.productId = productId;
        this.optionId = optionId;
        this.memberId = memberId;
        this.productName = productName;
        this.optionName = optionName;
        this.unitPrice = unitPrice;
        this.productImageUrl = productImageUrl;
        this.quantity = quantity;
        this.message = message;
        this.orderDateTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getProductName() {
        return productName;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getOrderDateTime() {
        return orderDateTime;
    }
}
