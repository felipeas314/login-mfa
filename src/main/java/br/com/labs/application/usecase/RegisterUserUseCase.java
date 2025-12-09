package br.com.labs.application.usecase;

import br.com.labs.domain.auth.PasswordEncoder;
import br.com.labs.domain.exception.UserAlreadyExistsException;
import br.com.labs.domain.user.Email;
import br.com.labs.domain.user.Password;
import br.com.labs.domain.user.User;
import br.com.labs.domain.user.UserRepository;
import br.com.labs.domain.user.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Output execute(Input input) {
        var username = new Username(input.username());
        var email = new Email(input.email());

        validateUniqueConstraints(username, email);

        Password.validateRawPassword(input.password());
        var hashedPassword = new Password(passwordEncoder.encode(input.password()));

        var user = User.create(username, email, hashedPassword);
        userRepository.save(user);

        return new Output(user.getId().toString(), user.getUsername().value(), user.getEmail().value());
    }

    private void validateUniqueConstraints(Username username, Email email) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email");
        }
    }

    public record Input(String username, String email, String password) {}

    public record Output(String userId, String username, String email) {}
}
