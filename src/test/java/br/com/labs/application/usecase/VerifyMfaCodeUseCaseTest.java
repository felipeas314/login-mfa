package br.com.labs.application.usecase;

import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.TokenPair;
import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.exception.InvalidTokenException;
import br.com.labs.domain.exception.MfaBlockedException;
import br.com.labs.domain.exception.MfaCodeExpiredException;
import br.com.labs.domain.exception.MfaCodeInvalidException;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyMfaCodeUseCaseTest {

    @Mock
    private MfaRepository mfaRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private VerifyMfaCodeUseCase useCase;

    private UserId userId;

    @BeforeEach
    void setUp() {
        useCase = new VerifyMfaCodeUseCase(mfaRepository, tokenRepository, jwtTokenProvider, 3);
        userId = UserId.generate();
    }

    @Test
    @DisplayName("Should verify MFA code and return tokens")
    void shouldVerifyMfaCodeAndReturnTokens() {
        var input = new VerifyMfaCodeUseCase.Input("mfa.token", "123456");
        var tokenPair = new TokenPair("access.token", "refresh.token", 900000, 604800000);

        when(jwtTokenProvider.validateMfaToken("mfa.token")).thenReturn(userId);
        when(mfaRepository.isBlocked(userId)).thenReturn(false);
        when(mfaRepository.findCode(userId)).thenReturn(Optional.of(new MfaCode("123456")));
        when(jwtTokenProvider.generateTokenPair(userId)).thenReturn(tokenPair);
        when(jwtTokenProvider.extractRefreshTokenId("refresh.token")).thenReturn("refresh-id");

        var output = useCase.execute(input);

        assertThat(output.accessToken()).isEqualTo("access.token");
        assertThat(output.refreshToken()).isEqualTo("refresh.token");

        verify(mfaRepository).deleteCode(userId);
        verify(tokenRepository).saveRefreshToken("refresh-id", userId);
    }

    @Test
    @DisplayName("Should throw exception when user is blocked")
    void shouldThrowExceptionWhenUserIsBlocked() {
        var input = new VerifyMfaCodeUseCase.Input("mfa.token", "123456");

        when(jwtTokenProvider.validateMfaToken("mfa.token")).thenReturn(userId);
        when(mfaRepository.isBlocked(userId)).thenReturn(true);
        when(mfaRepository.getBlockTtl(userId)).thenReturn(600L);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(MfaBlockedException.class)
                .hasMessageContaining("600");

        verify(mfaRepository, never()).findCode(any());
    }

    @Test
    @DisplayName("Should throw exception when MFA code expired")
    void shouldThrowExceptionWhenCodeExpired() {
        var input = new VerifyMfaCodeUseCase.Input("mfa.token", "123456");

        when(jwtTokenProvider.validateMfaToken("mfa.token")).thenReturn(userId);
        when(mfaRepository.isBlocked(userId)).thenReturn(false);
        when(mfaRepository.findCode(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(MfaCodeExpiredException.class);
    }

    @Test
    @DisplayName("Should increment attempts when code is wrong")
    void shouldIncrementAttemptsWhenCodeIsWrong() {
        var input = new VerifyMfaCodeUseCase.Input("mfa.token", "000000");

        when(jwtTokenProvider.validateMfaToken("mfa.token")).thenReturn(userId);
        when(mfaRepository.isBlocked(userId)).thenReturn(false);
        when(mfaRepository.findCode(userId)).thenReturn(Optional.of(new MfaCode("123456")));
        when(mfaRepository.incrementAttempts(userId)).thenReturn(1);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(MfaCodeInvalidException.class)
                .extracting("remainingAttempts")
                .isEqualTo(2);

        verify(tokenRepository, never()).saveRefreshToken(anyString(), any());
    }

    @Test
    @DisplayName("Should block user after max attempts")
    void shouldBlockUserAfterMaxAttempts() {
        var input = new VerifyMfaCodeUseCase.Input("mfa.token", "000000");

        when(jwtTokenProvider.validateMfaToken("mfa.token")).thenReturn(userId);
        when(mfaRepository.isBlocked(userId)).thenReturn(false);
        when(mfaRepository.findCode(userId)).thenReturn(Optional.of(new MfaCode("123456")));
        when(mfaRepository.incrementAttempts(userId)).thenReturn(3);
        when(mfaRepository.getBlockTtl(userId)).thenReturn(900L);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(MfaBlockedException.class);

        verify(mfaRepository).block(userId);
        verify(mfaRepository).deleteCode(userId);
    }

    @Test
    @DisplayName("Should throw exception when MFA token is invalid")
    void shouldThrowExceptionWhenMfaTokenIsInvalid() {
        var input = new VerifyMfaCodeUseCase.Input("invalid.token", "123456");

        when(jwtTokenProvider.validateMfaToken("invalid.token"))
                .thenThrow(new InvalidTokenException("Token expired"));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidTokenException.class);
    }
}
