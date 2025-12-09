package br.com.labs.domain.user;

import java.time.Instant;
import java.util.Objects;

public class User {

    private final UserId id;
    private final Username username;
    private final Email email;
    private Password password;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(UserId id, Username username, Email email, Password password, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    public static User create(Username username, Email email, Password password) {
        var now = Instant.now();
        return new User(
                UserId.generate(),
                username,
                email,
                password,
                now,
                now
        );
    }

    public static User reconstitute(
            UserId id,
            Username username,
            Email email,
            Password password,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new User(id, username, email, password, createdAt, updatedAt);
    }

    public void changePassword(Password newPassword) {
        this.password = Objects.requireNonNull(newPassword, "New password cannot be null");
        this.updatedAt = Instant.now();
    }

    public UserId getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id=%s, username=%s, email=%s}".formatted(id, username, email);
    }
}
