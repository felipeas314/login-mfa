package br.com.labs.infrastructure.persistence.jpa;

import br.com.labs.domain.user.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        var entity = UserJpaEntity.fromDomain(user);
        var savedEntity = jpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value())
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.value())
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return jpaRepository.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }
}
