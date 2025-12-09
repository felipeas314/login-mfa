package br.com.labs.domain.auth;

import java.security.SecureRandom;

public record MfaCode(String value) {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    public MfaCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MFA code cannot be null or blank");
        }
        if (!value.matches("^\\d{" + CODE_LENGTH + "}$")) {
            throw new IllegalArgumentException("MFA code must be exactly %d digits".formatted(CODE_LENGTH));
        }
    }

    public static MfaCode generate() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return new MfaCode(code.toString());
    }

    public boolean matches(String otherCode) {
        return value.equals(otherCode);
    }

    @Override
    public String toString() {
        return "[MFA CODE]";
    }
}
