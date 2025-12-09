package br.com.labs.infrastructure.security;

import br.com.labs.domain.auth.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordEncoder implements PasswordEncoder {

    private static final int LOG_ROUNDS = 12;

    @Override
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
