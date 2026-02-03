package br.com.labs.infrastructure.persistence.jpa;

import br.com.labs.domain.user.*;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {
    }

    public static UserJpaEntity fromDomain(User user) {
        return fromDomain(user, true);
    }

    public static UserJpaEntity fromDomain(User user, boolean isNew) {
        var entity = new UserJpaEntity();
        entity.id = user.getId().value();
        entity.username = user.getUsername().value();
        entity.email = user.getEmail().value();
        entity.passwordHash = user.getPassword().hashedValue();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        entity.isNew = isNew;
        return entity;
    }

    public User toDomain() {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                new Email(email),
                new Password(passwordHash),
                createdAt,
                updatedAt
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
