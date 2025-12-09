package br.com.labs.application.usecase;

import br.com.labs.domain.auth.TokenPair;
import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.exception.InvalidTokenException;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RefreshTokenUseCase useCase;

    private UserId userId;

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(tokenRepository, jwtTokenProvider);
        userId = UserId.generate();
    }

    @Test
    @DisplayName("Should refresh tokens successfully")
    void shouldRefreshTokensSuccessfully() {
        var input = new RefreshTokenUseCase.Input("old.refresh.token");
        var newTokenPair = new TokenPair("new.access.token", "new.refresh.token", 900000, 604800000);

        when(jwtTokenProvider.validateRefreshToken("old.refresh.token")).thenReturn(userId);
        when(jwtTokenProvider.extractRefreshTokenId("old.refresh.token")).thenReturn("old-refresh-id");
        when(tokenRepository.existsRefreshToken("old-refresh-id")).thenReturn(true);
        when(jwtTokenProvider.generateTokenPair(userId)).thenReturn(newTokenPair);
        when(jwtTokenProvider.extractRefreshTokenId("new.refresh.token")).thenReturn("new-refresh-id");

        var output = useCase.execute(input);

        assertThat(output.accessToken()).isEqualTo("new.access.token");
        assertThat(output.refreshToken()).isEqualTo("new.refresh.token");

        verify(tokenRepository).deleteRefreshToken("old-refresh-id");
        verify(tokenRepository).saveRefreshToken("new-refresh-id", userId);
    }

    @Test
    @DisplayName("Should throw exception when refresh token not in whitelist")
    void shouldThrowExceptionWhenTokenNotInWhitelist() {
        var input = new RefreshTokenUseCase.Input("revoked.refresh.token");

        when(jwtTokenProvider.validateRefreshToken("revoked.refresh.token")).thenReturn(userId);
        when(jwtTokenProvider.extractRefreshTokenId("revoked.refresh.token")).thenReturn("revoked-id");
        when(tokenRepository.existsRefreshToken("revoked-id")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not found or revoked");

        verify(tokenRepository, never()).saveRefreshToken(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void shouldThrowExceptionWhenTokenIsInvalid() {
        var input = new RefreshTokenUseCase.Input("invalid.token");

        when(jwtTokenProvider.validateRefreshToken("invalid.token"))
                .thenThrow(new InvalidTokenException("Token expired"));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidTokenException.class);
    }
}
