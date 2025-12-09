package br.com.labs.domain.user;

public record Password(String hashedValue) {

    public Password {
        if (hashedValue == null || hashedValue.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
    }

    public static void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (rawPassword.length() > 100) {
            throw new IllegalArgumentException("Password must be at most 100 characters long");
        }
        if (!rawPassword.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!rawPassword.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!rawPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
