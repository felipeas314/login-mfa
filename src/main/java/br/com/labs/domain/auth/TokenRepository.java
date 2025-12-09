package br.com.labs.domain.auth;

import br.com.labs.domain.user.UserId;

public interface TokenRepository {

    void saveRefreshToken(String tokenId, UserId userId);

    boolean existsRefreshToken(String tokenId);

    void deleteRefreshToken(String tokenId);

    void addToBlacklist(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);
}
