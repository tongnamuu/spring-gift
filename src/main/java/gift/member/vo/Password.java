package gift.member.vo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

public class Password {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final String value;

    private Password(String value) {
        this.value = Objects.requireNonNull(value, "Encoded password must not be null.");
    }

    public static Password encode(String plainText) {
        validatePlainText(plainText);
        return new Password(ENCODER.encode(plainText));
    }

    public static boolean matches(String plainText, String encodedPassword) {
        validatePlainText(plainText);
        return ENCODER.matches(plainText, encodedPassword);
    }

    public String value() {
        return value;
    }

    private static void validatePlainText(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Password that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
