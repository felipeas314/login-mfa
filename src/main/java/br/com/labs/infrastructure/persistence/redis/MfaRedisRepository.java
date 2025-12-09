package br.com.labs.infrastructure.persistence.redis;

import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.user.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class MfaRedisRepository implements MfaRepository {

    private static final String MFA_CODE_PREFIX = "mfa:code:";
    private static final String MFA_ATTEMPTS_PREFIX = "mfa:attempts:";
    private static final String MFA_BLOCK_PREFIX = "mfa:block:";

    private final StringRedisTemplate redisTemplate;
    private final long codeTtlSeconds;
    private final long blockTtlSeconds;

    public MfaRedisRepository(
            StringRedisTemplate redisTemplate,
            @Value("${mfa.code.ttl}") long codeTtlSeconds,
            @Value("${mfa.block.ttl}") long blockTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.codeTtlSeconds = codeTtlSeconds;
        this.blockTtlSeconds = blockTtlSeconds;
    }

    @Override
    public void saveCode(UserId userId, MfaCode code) {
        String key = MFA_CODE_PREFIX + userId.value();
        redisTemplate.opsForValue().set(key, code.value(), codeTtlSeconds, TimeUnit.SECONDS);

        String attemptsKey = MFA_ATTEMPTS_PREFIX + userId.value();
        redisTemplate.opsForValue().set(attemptsKey, "0", codeTtlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<MfaCode> findCode(UserId userId) {
        String key = MFA_CODE_PREFIX + userId.value();
        String code = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(code).map(MfaCode::new);
    }

    @Override
    public void deleteCode(UserId userId) {
        String codeKey = MFA_CODE_PREFIX + userId.value();
        String attemptsKey = MFA_ATTEMPTS_PREFIX + userId.value();
        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);
    }

    @Override
    public int incrementAttempts(UserId userId) {
        String key = MFA_ATTEMPTS_PREFIX + userId.value();
        Long attempts = redisTemplate.opsForValue().increment(key);
        return attempts != null ? attempts.intValue() : 0;
    }

    @Override
    public int getAttempts(UserId userId) {
        String key = MFA_ATTEMPTS_PREFIX + userId.value();
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }

    @Override
    public void block(UserId userId) {
        String key = MFA_BLOCK_PREFIX + userId.value();
        redisTemplate.opsForValue().set(key, "blocked", blockTtlSeconds, TimeUnit.SECONDS);

        deleteCode(userId);
    }

    @Override
    public boolean isBlocked(UserId userId) {
        String key = MFA_BLOCK_PREFIX + userId.value();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public long getBlockTtl(UserId userId) {
        String key = MFA_BLOCK_PREFIX + userId.value();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }
}
