package gift.member.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointAmountTest {
    @Test
    void pointAmountKeepsPositiveValue() {
        PointAmount amount = new PointAmount(3000);

        assertThat(amount.value()).isEqualTo(3000);
    }

    @Test
    void pointAmountRejectsZeroOrNegativeValue() {
        assertThatThrownBy(() -> new PointAmount(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount must be greater than zero.");
        assertThatThrownBy(() -> new PointAmount(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount must be greater than zero.");
    }
}
