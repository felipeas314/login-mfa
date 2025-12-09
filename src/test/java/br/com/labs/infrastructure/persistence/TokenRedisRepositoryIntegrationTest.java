package br.com.labs.infrastructure.persistence;

import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRedisRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TokenRepository tokenRepository;

    @Test
    @DisplayName("Should save and check refresh token existence")
    void shouldSaveAndCheckRefreshToken() {
        var userId = UserId.generate();
        var tokenId = "refresh-token-id-123";

        tokenRepository.saveRefreshToken(tokenId, userId);

        assertThat(tokenRepository.existsRefreshToken(tokenId)).isTrue();
        assertThat(tokenRepository.existsRefreshToken("non-existent")).isFalse();
    }

    @Test
    @DisplayName("Should delete refresh token")
    void shouldDeleteRefreshToken() {
        var userId = UserId.generate();
        var tokenId = "refresh-to-delete";

        tokenRepository.saveRefreshToken(tokenId, userId);
        assertThat(tokenRepository.existsRefreshToken(tokenId)).isTrue();

        tokenRepository.deleteRefreshToken(tokenId);

        assertThat(tokenRepository.existsRefreshToken(tokenId)).isFalse();
    }

    @Test
    @DisplayName("Should add to blacklist and check")
    void shouldAddToBlacklistAndCheck() {
        var jti = "access-token-jti-456";

        assertThat(tokenRepository.isBlacklisted(jti)).isFalse();

        tokenRepository.addToBlacklist(jti, 60);

        assertThat(tokenRepository.isBlacklisted(jti)).isTrue();
    }
}
