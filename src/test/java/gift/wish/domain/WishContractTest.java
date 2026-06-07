package gift.wish.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WishContractTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @Test
    void wishConnectsMemberIdAndProductIdWithoutDirectEntityReferences() {
        Wish wish = new Wish(MEMBER_ID, PRODUCT_ID);

        assertThat(wish.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(wish.getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void wishIsOwnedByMatchingMemberId() {
        Wish wish = new Wish(MEMBER_ID, PRODUCT_ID);

        assertThat(wish.isOwnedBy(MEMBER_ID)).isTrue();
        assertThat(wish.isOwnedBy(2L)).isFalse();
    }
}
