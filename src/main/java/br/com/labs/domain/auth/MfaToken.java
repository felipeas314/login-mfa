package br.com.labs.domain.auth;

public record MfaToken(String value, long expiresIn) {

    public MfaToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MFA token cannot be null or blank");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("MFA token expiration must be positive");
        }
    }
}
