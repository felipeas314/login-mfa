package br.com.labs.application.usecase;

import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.TokenPair;
import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.exception.MfaBlockedException;
import br.com.labs.domain.exception.MfaCodeExpiredException;
import br.com.labs.domain.exception.MfaCodeInvalidException;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerifyMfaCodeUseCase {

    private final MfaRepository mfaRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final int maxAttempts;

    public VerifyMfaCodeUseCase(
            MfaRepository mfaRepository,
            TokenRepository tokenRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${mfa.block.max-attempts}") int maxAttempts
    ) {
        this.mfaRepository = mfaRepository;
        this.tokenRepository = tokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.maxAttempts = maxAttempts;
    }

    public Output execute(Input input) {
        UserId userId = jwtTokenProvider.validateMfaToken(input.mfaToken());

        checkIfBlocked(userId);

        var storedCode = mfaRepository.findCode(userId)
                .orElseThrow(MfaCodeExpiredException::new);

        if (!storedCode.matches(input.code())) {
            handleFailedAttempt(userId);
        }

        mfaRepository.deleteCode(userId);

        TokenPair tokenPair = jwtTokenProvider.generateTokenPair(userId);

        String refreshTokenId = jwtTokenProvider.extractRefreshTokenId(tokenPair.refreshToken());
        tokenRepository.saveRefreshToken(refreshTokenId, userId);

        return new Output(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.accessTokenExpiresIn(),
                tokenPair.refreshTokenExpiresIn()
        );
    }

    private void checkIfBlocked(UserId userId) {
        if (mfaRepository.isBlocked(userId)) {
            long ttl = mfaRepository.getBlockTtl(userId);
            throw new MfaBlockedException(ttl);
        }
    }

    private void handleFailedAttempt(UserId userId) {
        int attempts = mfaRepository.incrementAttempts(userId);

        if (attempts >= maxAttempts) {
            mfaRepository.block(userId);
            mfaRepository.deleteCode(userId);
            long ttl = mfaRepository.getBlockTtl(userId);
            throw new MfaBlockedException(ttl);
        }

        int remaining = maxAttempts - attempts;
        throw new MfaCodeInvalidException(remaining);
    }

    public record Input(String mfaToken, String code) {}

    public record Output(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn
    ) {}
}
