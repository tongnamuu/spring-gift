package gift.member.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {
    @Test
    void passwordKeepsEncodedValue() {
        Password password = Password.encode("password123");

        assertThat(password.value()).isNotEqualTo("password123");
        assertThat(password.matches(password.value())).isTrue();
    }

    @Test
    void passwordRejectsBlankValue() {
        assertThatThrownBy(() -> Password.encode(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Password must not be blank.");
    }

    @Test
    void passwordDoesNotExposeRawValueFromToString() {
        Password password = Password.encode("password123");

        assertThat(password.toString()).doesNotContain("password123");
    }

    @Test
    void passwordMatchesSeedBcryptHash() {
        Password password = Password.encode("admin1234");

        assertThat(password.matches("$2y$10$MY0SCnICoAGEcs6bJ8iwd.p9IPUzDfjWYcTsybSkd.jPLCXuSW7La")).isTrue();
    }
}
