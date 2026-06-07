package gift.member.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberContractTest {
    @Test
    void memberStartsWithZeroPoint() {
        Member member = new Member("member-contract@example.com", "password123");

        assertThat(member.getPoint()).isZero();
    }

    @Test
    void memberCanChargeAndDeductPoint() {
        Member member = new Member("member-contract@example.com", "password123");

        member.chargePoint(3000);
        member.deductPoint(1000);

        assertThat(member.getPoint()).isEqualTo(2000);
    }

    @Test
    void chargePointRequiresPositiveAmount() {
        Member member = new Member("member-contract@example.com", "password123");

        assertThatThrownBy(() -> member.chargePoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Amount must be greater than zero.");
    }

    @Test
    void deductPointRequiresPositiveAmount() {
        Member member = new Member("member-contract@example.com", "password123");

        assertThatThrownBy(() -> member.deductPoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("차감 금액은 1 이상이어야 합니다.");
    }

    @Test
    void deductPointRejectsInsufficientPoint() {
        Member member = new Member("member-contract@example.com", "password123");
        member.chargePoint(1000);

        assertThatThrownBy(() -> member.deductPoint(1001))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("포인트가 부족합니다.");
    }

    @Test
    void memberKeepsKakaoAccessTokenAsMemberState() {
        Member member = new Member("member-contract@example.com", "password123");

        member.updateKakaoAccessToken("kakao-access-token");

        assertThat(member.getKakaoAccessToken()).isEqualTo("kakao-access-token");
    }
}
