package br.com.labs.domain.exception;

public final class InvalidTokenException extends DomainException {

    public InvalidTokenException(String reason) {
        super("TOKEN_001", "Invalid token: %s".formatted(reason));
    }

    public InvalidTokenException() {
        super("TOKEN_001", "Invalid or expired token");
    }
}
