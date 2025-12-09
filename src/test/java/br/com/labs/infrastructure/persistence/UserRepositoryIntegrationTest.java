package br.com.labs.infrastructure.persistence;

import br.com.labs.domain.user.Email;
import br.com.labs.domain.user.Password;
import br.com.labs.domain.user.User;
import br.com.labs.domain.user.UserRepository;
import br.com.labs.domain.user.Username;
import br.com.labs.infrastructure.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and find user by id")
    void shouldSaveAndFindById() {
        var user = User.create(
                new Username("integration.test"),
                new Email("integration@test.com"),
                new Password("hashed_password")
        );

        var savedUser = userRepository.save(user);

        var foundUser = userRepository.findById(savedUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername().value()).isEqualTo("integration.test");
        assertThat(foundUser.get().getEmail().value()).isEqualTo("integration@test.com");
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindByUsername() {
        var user = User.create(
                new Username("find.by.username"),
                new Email("findbyusername@test.com"),
                new Password("hashed_password")
        );

        userRepository.save(user);

        var foundUser = userRepository.findByUsername(new Username("find.by.username"));

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail().value()).isEqualTo("findbyusername@test.com");
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        var user = User.create(
                new Username("find.by.email"),
                new Email("findbyemail@test.com"),
                new Password("hashed_password")
        );

        userRepository.save(user);

        var foundUser = userRepository.findByEmail(new Email("findbyemail@test.com"));

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername().value()).isEqualTo("find.by.email");
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckUsernameExists() {
        var user = User.create(
                new Username("exists.check"),
                new Email("existscheck@test.com"),
                new Password("hashed_password")
        );

        userRepository.save(user);

        assertThat(userRepository.existsByUsername(new Username("exists.check"))).isTrue();
        assertThat(userRepository.existsByUsername(new Username("not.exists"))).isFalse();
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckEmailExists() {
        var user = User.create(
                new Username("email.exists"),
                new Email("emailexists@test.com"),
                new Password("hashed_password")
        );

        userRepository.save(user);

        assertThat(userRepository.existsByEmail(new Email("emailexists@test.com"))).isTrue();
        assertThat(userRepository.existsByEmail(new Email("notexists@test.com"))).isFalse();
    }
}
