package br.com.labs.application.usecase;

import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private LogoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(tokenRepository, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should logout with access and refresh tokens")
    void shouldLogoutWithBothTokens() {
        var input = new LogoutUseCase.Input("access.token", "refresh.token");

        when(jwtTokenProvider.extractJti("access.token")).thenReturn("access-jti");
        when(jwtTokenProvider.getRemainingTtlSeconds("access.token")).thenReturn(600L);
        when(jwtTokenProvider.extractRefreshTokenId("refresh.token")).thenReturn("refresh-id");

        useCase.execute(input);

        verify(tokenRepository).addToBlacklist("access-jti", 600L);
        verify(tokenRepository).deleteRefreshToken("refresh-id");
    }

    @Test
    @DisplayName("Should logout with only access token")
    void shouldLogoutWithOnlyAccessToken() {
        var input = new LogoutUseCase.Input("access.token");

        when(jwtTokenProvider.extractJti("access.token")).thenReturn("access-jti");
        when(jwtTokenProvider.getRemainingTtlSeconds("access.token")).thenReturn(600L);

        useCase.execute(input);

        verify(tokenRepository).addToBlacklist("access-jti", 600L);
        verify(tokenRepository, never()).deleteRefreshToken(null);
    }

    @Test
    @DisplayName("Should not blacklist expired access token")
    void shouldNotBlacklistExpiredToken() {
        var input = new LogoutUseCase.Input("expired.access.token", "refresh.token");

        when(jwtTokenProvider.extractJti("expired.access.token")).thenReturn("access-jti");
        when(jwtTokenProvider.getRemainingTtlSeconds("expired.access.token")).thenReturn(0L);
        when(jwtTokenProvider.extractRefreshTokenId("refresh.token")).thenReturn("refresh-id");

        useCase.execute(input);

        verify(tokenRepository, never()).addToBlacklist("access-jti", 0L);
        verify(tokenRepository).deleteRefreshToken("refresh-id");
    }
}
