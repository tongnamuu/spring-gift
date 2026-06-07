package gift.member.vo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

public class Password {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final String value;
    private final String rawValue;

    private Password(String value, String rawValue) {
        this.value = Objects.requireNonNull(value, "Encoded password must not be null.");
        this.rawValue = rawValue;
    }

    public static Password encode(String rawValue) {
        validateRaw(rawValue);
        return new Password(ENCODER.encode(rawValue), rawValue);
    }

    public String value() {
        return value;
    }

    public boolean matches(String encodedPassword) {
        if (rawValue == null) {
            throw new IllegalStateException("Raw password is required for password matching.");
        }
        return ENCODER.matches(rawValue, encodedPassword);
    }

    private static void validateRaw(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
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
