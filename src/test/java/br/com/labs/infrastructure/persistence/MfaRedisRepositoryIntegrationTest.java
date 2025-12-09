package br.com.labs.infrastructure.persistence;

import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class MfaRedisRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MfaRepository mfaRepository;

    @Test
    @DisplayName("Should save and find MFA code")
    void shouldSaveAndFindMfaCode() {
        var userId = UserId.generate();
        var code = new MfaCode("123456");

        mfaRepository.saveCode(userId, code);

        var foundCode = mfaRepository.findCode(userId);

        assertThat(foundCode).isPresent();
        assertThat(foundCode.get().value()).isEqualTo("123456");
    }

    @Test
    @DisplayName("Should delete MFA code")
    void shouldDeleteMfaCode() {
        var userId = UserId.generate();
        var code = new MfaCode("654321");

        mfaRepository.saveCode(userId, code);
        assertThat(mfaRepository.findCode(userId)).isPresent();

        mfaRepository.deleteCode(userId);

        assertThat(mfaRepository.findCode(userId)).isEmpty();
    }

    @Test
    @DisplayName("Should increment attempts")
    void shouldIncrementAttempts() {
        var userId = UserId.generate();

        assertThat(mfaRepository.getAttempts(userId)).isZero();

        int attempts1 = mfaRepository.incrementAttempts(userId);
        assertThat(attempts1).isEqualTo(1);

        int attempts2 = mfaRepository.incrementAttempts(userId);
        assertThat(attempts2).isEqualTo(2);

        assertThat(mfaRepository.getAttempts(userId)).isEqualTo(2);
    }

    @Test
    @DisplayName("Should block and check blocked status")
    void shouldBlockAndCheckStatus() {
        var userId = UserId.generate();

        assertThat(mfaRepository.isBlocked(userId)).isFalse();

        mfaRepository.block(userId);

        assertThat(mfaRepository.isBlocked(userId)).isTrue();
        assertThat(mfaRepository.getBlockTtl(userId)).isPositive();
    }
}
