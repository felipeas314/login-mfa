package br.com.labs.domain.auth;

import br.com.labs.domain.user.UserId;

import java.util.Optional;

public interface MfaRepository {

    void saveCode(UserId userId, MfaCode code);

    Optional<MfaCode> findCode(UserId userId);

    void deleteCode(UserId userId);

    int incrementAttempts(UserId userId);

    int getAttempts(UserId userId);

    void block(UserId userId);

    boolean isBlocked(UserId userId);

    long getBlockTtl(UserId userId);
}
