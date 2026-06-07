package gift.product.entity;

import gift.product.vo.ProductName;
import gift.product.vo.ProductPrice;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductContractTest {
    @Test
    void productReferencesCategoryById() {
        Product product = new Product(
            new ProductName("테스트 상품"),
            new ProductPrice(10000),
            "https://example.com/product.png",
            1L
        );

        assertThat(product.getName()).isEqualTo("테스트 상품");
        assertThat(product.getPrice()).isEqualTo(10000);
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/product.png");
        assertThat(product.getCategoryId()).isEqualTo(1L);
    }

    @Test
    void productCanBeCreatedWithoutOptionsBeforeOptionsAreRegistered() {
        Product product = product("옵션 등록 전 상품");

        assertThat(product.getOptions()).isEmpty();
    }

    @Test
    void updateChangesProductAttributes() {
        Product product = product("기존 상품");

        product.update(
            new ProductName("변경 상품"),
            new ProductPrice(20000),
            "https://example.com/new-product.png",
            2L
        );

        assertThat(product.getName()).isEqualTo("변경 상품");
        assertThat(product.getPrice()).isEqualTo(20000);
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/new-product.png");
        assertThat(product.getCategoryId()).isEqualTo(2L);
    }

    @Test
    void updateRefreshesUpdateDateTime() {
        Product product = product("수정 시간 상품");
        LocalDateTime previousUpdateDt = LocalDateTime.of(2026, 1, 1, 0, 0);
        setUpdateDt(product, previousUpdateDt);

        product.update(
            new ProductName("수정된 상품"),
            new ProductPrice(20000),
            "https://example.com/updated-product.png",
            2L
        );

        assertThat(product.getUpdateDt()).isAfter(previousUpdateDt);
    }

    @Test
    void productAddsOptionAsOwnedEntity() {
        Product product = product("옵션 상품");

        Option option = product.addOption("기본 옵션", 10);

        assertThat(option.getProduct()).isSameAs(product);
        assertThat(option.getName()).isEqualTo("기본 옵션");
        assertThat(option.getQuantity()).isEqualTo(10);
        assertThat(product.getOptions()).containsExactly(option);
    }

    @Test
    void addOptionRefreshesProductUpdateDateTime() {
        Product product = product("옵션 추가 시간 상품");
        LocalDateTime previousUpdateDt = LocalDateTime.of(2026, 1, 1, 0, 0);
        setUpdateDt(product, previousUpdateDt);

        product.addOption("추가 옵션", 10);

        assertThat(product.getUpdateDt()).isAfter(previousUpdateDt);
    }

    @Test
    void productDoesNotAllowDuplicateOptionNames() {
        Product product = product("중복 옵션 상품");
        product.addOption("동일 옵션", 10);

        assertThatThrownBy(() -> product.addOption("동일 옵션", 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 옵션명입니다.");
    }

    @Test
    void productRemovesOwnedOptionWhenMoreThanOneOptionExists() {
        Product product = product("옵션 삭제 상품");
        Option first = product.addOption("첫 번째 옵션", 10);
        Option second = product.addOption("두 번째 옵션", 20);

        product.removeOption(first);

        assertThat(product.getOptions()).containsExactly(second);
    }

    @Test
    void removeOptionRefreshesProductUpdateDateTime() {
        Product product = product("옵션 삭제 시간 상품");
        Option first = product.addOption("첫 번째 옵션", 10);
        product.addOption("두 번째 옵션", 20);
        LocalDateTime previousUpdateDt = LocalDateTime.of(2026, 1, 1, 0, 0);
        setUpdateDt(product, previousUpdateDt);

        product.removeOption(first);

        assertThat(product.getUpdateDt()).isAfter(previousUpdateDt);
    }

    @Test
    void productDoesNotRemoveLastOption() {
        Product product = product("마지막 옵션 상품");
        Option option = product.addOption("마지막 옵션", 10);

        assertThatThrownBy(() -> product.removeOption(option))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
    }

    @Test
    void productDoesNotRemoveOptionOwnedByAnotherProduct() {
        Product product = product("옵션 소유 상품");
        product.addOption("소유 옵션", 10);
        product.addOption("남길 옵션", 20);
        Option otherProductOption = product("다른 상품").addOption("다른 상품 옵션", 10);

        assertThatThrownBy(() -> product.removeOption(otherProductOption))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품에 속하지 않은 옵션입니다.");
    }

    @Test
    void productSubtractsOwnedOptionQuantity() {
        Product product = product("옵션 재고 상품");
        Option option = product.addOption("재고 옵션", 10);
        setId(option, 1L);

        Option updated = product.subtractOptionQuantity(1L, 3);

        assertThat(updated).isSameAs(option);
        assertThat(option.getQuantity()).isEqualTo(7);
    }

    @Test
    void productDoesNotSubtractOptionQuantityWhenOptionIsNotOwned() {
        Product product = product("옵션 재고 소유 상품");
        Option option = product.addOption("재고 옵션", 10);
        setId(option, 1L);

        assertThatThrownBy(() -> product.subtractOptionQuantity(2L, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품에 속하지 않은 옵션입니다.");
        assertThat(option.getQuantity()).isEqualTo(10);
    }

    private Product product(String name) {
        return new Product(
            new ProductName(name),
            new ProductPrice(10000),
            "https://example.com/product.png",
            1L
        );
    }

    private void setId(Option option, Long id) {
        try {
            Field field = Option.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(option, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to assign option id for contract test.", e);
        }
    }

    private void setUpdateDt(Product product, LocalDateTime updateDt) {
        try {
            Field field = Product.class.getDeclaredField("updateDt");
            field.setAccessible(true);
            field.set(product, updateDt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to assign product update date time for contract test.", e);
        }
    }
}
