package br.com.labs.application.usecase;

import br.com.labs.domain.auth.EmailSender;
import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.MfaToken;
import br.com.labs.domain.auth.PasswordEncoder;
import br.com.labs.domain.exception.InvalidCredentialsException;
import br.com.labs.domain.user.User;
import br.com.labs.domain.user.UserRepository;
import br.com.labs.domain.user.Username;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MfaRepository mfaRepository;
    private final EmailSender emailSender;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticateUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MfaRepository mfaRepository,
            EmailSender emailSender,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mfaRepository = mfaRepository;
        this.emailSender = emailSender;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Output execute(Input input) {
        var username = new Username(input.username());

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(input.password(), user.getPassword().hashedValue())) {
            throw new InvalidCredentialsException();
        }

        var mfaCode = MfaCode.generate();
        mfaRepository.saveCode(user.getId(), mfaCode);

        emailSender.sendMfaCode(user.getEmail(), mfaCode);

        MfaToken mfaToken = jwtTokenProvider.generateMfaToken(user.getId());

        return new Output(mfaToken.value(), mfaToken.expiresIn());
    }

    public record Input(String username, String password) {}

    public record Output(String mfaToken, long expiresIn) {}
}
