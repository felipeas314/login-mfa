package br.com.labs.domain.auth;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {

    public TokenPair {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be null or blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or blank");
        }
        if (accessTokenExpiresIn <= 0) {
            throw new IllegalArgumentException("Access token expiration must be positive");
        }
        if (refreshTokenExpiresIn <= 0) {
            throw new IllegalArgumentException("Refresh token expiration must be positive");
        }
    }
}
