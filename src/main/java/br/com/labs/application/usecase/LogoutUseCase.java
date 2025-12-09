package br.com.labs.application.usecase;

import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class LogoutUseCase {

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public LogoutUseCase(TokenRepository tokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.tokenRepository = tokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void execute(Input input) {
        String accessJti = jwtTokenProvider.extractJti(input.accessToken());
        long accessTtl = jwtTokenProvider.getRemainingTtlSeconds(input.accessToken());

        if (accessTtl > 0) {
            tokenRepository.addToBlacklist(accessJti, accessTtl);
        }

        if (input.refreshToken() != null && !input.refreshToken().isBlank()) {
            String refreshTokenId = jwtTokenProvider.extractRefreshTokenId(input.refreshToken());
            tokenRepository.deleteRefreshToken(refreshTokenId);
        }
    }

    public record Input(String accessToken, String refreshToken) {
        public Input(String accessToken) {
            this(accessToken, null);
        }
    }
}
