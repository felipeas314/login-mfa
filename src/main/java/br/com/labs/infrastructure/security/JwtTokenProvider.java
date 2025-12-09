package br.com.labs.infrastructure.security;

import br.com.labs.domain.auth.MfaToken;
import br.com.labs.domain.auth.TokenPair;
import br.com.labs.domain.exception.InvalidTokenException;
import br.com.labs.domain.user.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String TOKEN_TYPE_MFA = "mfa";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long mfaTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token.expiration}") long refreshTokenExpiration,
            @Value("${jwt.mfa-token.expiration}") long mfaTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.mfaTokenExpiration = mfaTokenExpiration;
    }

    public TokenPair generateTokenPair(UserId userId) {
        var now = Instant.now();

        var accessToken = buildToken(userId, TOKEN_TYPE_ACCESS, now, accessTokenExpiration);
        var refreshToken = buildToken(userId, TOKEN_TYPE_REFRESH, now, refreshTokenExpiration);

        return new TokenPair(accessToken, refreshToken, accessTokenExpiration, refreshTokenExpiration);
    }

    public MfaToken generateMfaToken(UserId userId) {
        var now = Instant.now();
        var token = buildToken(userId, TOKEN_TYPE_MFA, now, mfaTokenExpiration);
        return new MfaToken(token, mfaTokenExpiration);
    }

    public UserId validateAccessToken(String token) {
        return validateToken(token, TOKEN_TYPE_ACCESS);
    }

    public UserId validateRefreshToken(String token) {
        return validateToken(token, TOKEN_TYPE_REFRESH);
    }

    public UserId validateMfaToken(String token) {
        return validateToken(token, TOKEN_TYPE_MFA);
    }

    public String extractJti(String token) {
        try {
            var claims = parseToken(token);
            return claims.getId();
        } catch (JwtException e) {
            throw new InvalidTokenException("Cannot extract JTI");
        }
    }

    public long getRemainingTtlSeconds(String token) {
        try {
            var claims = parseToken(token);
            var expiration = claims.getExpiration().toInstant();
            var remaining = expiration.getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (JwtException e) {
            throw new InvalidTokenException("Cannot calculate TTL");
        }
    }

    public String extractRefreshTokenId(String token) {
        try {
            var claims = parseToken(token);
            var type = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_REFRESH.equals(type)) {
                throw new InvalidTokenException("Not a refresh token");
            }
            return claims.getId();
        } catch (JwtException e) {
            throw new InvalidTokenException("Cannot extract refresh token ID");
        }
    }

    private String buildToken(UserId userId, String tokenType, Instant now, long expirationMs) {
        var expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    private UserId validateToken(String token, String expectedType) {
        try {
            var claims = parseToken(token);
            var type = claims.get(CLAIM_TOKEN_TYPE, String.class);

            if (!expectedType.equals(type)) {
                throw new InvalidTokenException("Invalid token type");
            }

            return UserId.from(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token expired");
        } catch (JwtException e) {
            throw new InvalidTokenException("Malformed token");
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
