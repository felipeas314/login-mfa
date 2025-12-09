package br.com.labs.domain.exception;

public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("AUTH_001", "Invalid username or password");
    }
}
