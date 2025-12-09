package br.com.labs.infrastructure.persistence.redis;

import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.user.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class TokenRedisRepository implements TokenRepository {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenTtlMs;

    public TokenRedisRepository(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token.expiration}") long refreshTokenTtlMs
    ) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    @Override
    public void saveRefreshToken(String tokenId, UserId userId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        long ttlSeconds = refreshTokenTtlMs / 1000;
        redisTemplate.opsForValue().set(key, userId.toString(), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean existsRefreshToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void deleteRefreshToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        redisTemplate.delete(key);
    }

    @Override
    public void addToBlacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
