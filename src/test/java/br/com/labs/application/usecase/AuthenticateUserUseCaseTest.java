package br.com.labs.application.usecase;

import br.com.labs.domain.auth.EmailSender;
import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.MfaToken;
import br.com.labs.domain.auth.PasswordEncoder;
import br.com.labs.domain.exception.InvalidCredentialsException;
import br.com.labs.domain.user.Email;
import br.com.labs.domain.user.Password;
import br.com.labs.domain.user.User;
import br.com.labs.domain.user.UserRepository;
import br.com.labs.domain.user.Username;
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
class AuthenticateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MfaRepository mfaRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthenticateUserUseCase useCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCase(
                userRepository,
                passwordEncoder,
                mfaRepository,
                emailSender,
                jwtTokenProvider
        );

        testUser = User.create(
                new Username("john.doe"),
                new Email("john@example.com"),
                new Password("hashed_password")
        );
    }

    @Test
    @DisplayName("Should authenticate user and send MFA code")
    void shouldAuthenticateAndSendMfaCode() {
        var input = new AuthenticateUserUseCase.Input("john.doe", "Password123");

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateMfaToken(any())).thenReturn(new MfaToken("mfa.token.here", 300000));

        var output = useCase.execute(input);

        assertThat(output.mfaToken()).isEqualTo("mfa.token.here");
        assertThat(output.expiresIn()).isEqualTo(300000);

        verify(mfaRepository).saveCode(any(), any(MfaCode.class));
        verify(emailSender).sendMfaCode(any(Email.class), any(MfaCode.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        var input = new AuthenticateUserUseCase.Input("unknown.user", "Password123");

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(mfaRepository, never()).saveCode(any(), any());
        verify(emailSender, never()).sendMfaCode(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when password is wrong")
    void shouldThrowExceptionWhenPasswordIsWrong() {
        var input = new AuthenticateUserUseCase.Input("john.doe", "WrongPassword123");

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(mfaRepository, never()).saveCode(any(), any());
        verify(emailSender, never()).sendMfaCode(any(), any());
    }
}
