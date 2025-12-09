package br.com.labs.infrastructure.web.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
    public TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn, long refreshTokenExpiresIn) {
        this(accessToken, refreshToken, "Bearer", accessTokenExpiresIn, refreshTokenExpiresIn);
    }
}
