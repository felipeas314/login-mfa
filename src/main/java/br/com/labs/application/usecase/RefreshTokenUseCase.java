package br.com.labs.application.usecase;

import br.com.labs.domain.auth.TokenPair;
import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.exception.InvalidTokenException;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUseCase {

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenUseCase(TokenRepository tokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.tokenRepository = tokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Output execute(Input input) {
        UserId userId = jwtTokenProvider.validateRefreshToken(input.refreshToken());

        String oldTokenId = jwtTokenProvider.extractRefreshTokenId(input.refreshToken());

        if (!tokenRepository.existsRefreshToken(oldTokenId)) {
            throw new InvalidTokenException("Refresh token not found or revoked");
        }

        tokenRepository.deleteRefreshToken(oldTokenId);

        TokenPair newTokenPair = jwtTokenProvider.generateTokenPair(userId);

        String newRefreshTokenId = jwtTokenProvider.extractRefreshTokenId(newTokenPair.refreshToken());
        tokenRepository.saveRefreshToken(newRefreshTokenId, userId);

        return new Output(
                newTokenPair.accessToken(),
                newTokenPair.refreshToken(),
                newTokenPair.accessTokenExpiresIn(),
                newTokenPair.refreshTokenExpiresIn()
        );
    }

    public record Input(String refreshToken) {}

    public record Output(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn
    ) {}
}
