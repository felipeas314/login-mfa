package br.com.labs.domain.exception;

public final class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException(String field) {
        super("USER_001", "User with this %s already exists".formatted(field));
    }
}
