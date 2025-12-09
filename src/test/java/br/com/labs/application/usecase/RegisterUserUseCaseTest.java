package br.com.labs.application.usecase;

import br.com.labs.domain.auth.PasswordEncoder;
import br.com.labs.domain.exception.UserAlreadyExistsException;
import br.com.labs.domain.user.Email;
import br.com.labs.domain.user.User;
import br.com.labs.domain.user.UserRepository;
import br.com.labs.domain.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        var input = new RegisterUserUseCase.Input("john.doe", "john@example.com", "Password123");

        when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var output = useCase.execute(input);

        assertThat(output.username()).isEqualTo("john.doe");
        assertThat(output.email()).isEqualTo("john@example.com");
        assertThat(output.userId()).isNotBlank();

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword().hashedValue()).isEqualTo("hashed_password");
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
        var input = new RegisterUserUseCase.Input("existing.user", "john@example.com", "Password123");

        when(userRepository.existsByUsername(any(Username.class))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        var input = new RegisterUserUseCase.Input("john.doe", "existing@example.com", "Password123");

        when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when password is weak")
    void shouldThrowExceptionWhenPasswordIsWeak() {
        var input = new RegisterUserUseCase.Input("john.doe", "john@example.com", "weak");

        when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }
}
