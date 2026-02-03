package br.com.labs.application.usecase;

import br.com.labs.application.service.SecurityMonitoringService;
import br.com.labs.domain.auth.EmailSender;
import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.MfaToken;
import br.com.labs.domain.auth.PasswordEncoder;
import br.com.labs.domain.exception.InvalidCredentialsException;
import br.com.labs.domain.exception.MfaBlockedException;
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
    private final SecurityMonitoringService securityMonitoringService;

    public AuthenticateUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MfaRepository mfaRepository,
            EmailSender emailSender,
            JwtTokenProvider jwtTokenProvider,
            SecurityMonitoringService securityMonitoringService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mfaRepository = mfaRepository;
        this.emailSender = emailSender;
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityMonitoringService = securityMonitoringService;
    }

    public Output execute(Input input) {
        var username = new Username(input.username());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // Não temos userId, mas registramos a tentativa com username
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(input.password(), user.getPassword().hashedValue())) {
            securityMonitoringService.recordLoginFailure(
                    user.getId(),
                    input.ipAddress(),
                    "Invalid password"
            );
            throw new InvalidCredentialsException();
        }

        checkIfBlocked(user);

        var mfaCode = MfaCode.generate();
        mfaRepository.saveCode(user.getId(), mfaCode);

        emailSender.sendMfaCode(user.getEmail(), mfaCode);

        MfaToken mfaToken = jwtTokenProvider.generateMfaToken(user.getId());

        return new Output(mfaToken.value(), mfaToken.expiresIn());
    }

    private void checkIfBlocked(User user) {
        if (mfaRepository.isBlocked(user.getId())) {
            long ttl = mfaRepository.getBlockTtl(user.getId());
            throw new MfaBlockedException(ttl);
        }
    }

    public record Input(String username, String password, String ipAddress) {
        public Input(String username, String password) {
            this(username, password, "unknown");
        }
    }

    public record Output(String mfaToken, long expiresIn) {}
}
