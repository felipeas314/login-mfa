package br.com.labs.domain.exception;

public final class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super("USER_002", "User not found");
    }
}
