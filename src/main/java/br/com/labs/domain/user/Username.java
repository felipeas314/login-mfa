package br.com.labs.domain.user;

import java.util.regex.Pattern;

public record Username(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be between %d and %d characters".formatted(MIN_LENGTH, MAX_LENGTH)
            );
        }
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Username can only contain letters, numbers, dots, underscores and hyphens"
            );
        }
        value = value.toLowerCase().trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
