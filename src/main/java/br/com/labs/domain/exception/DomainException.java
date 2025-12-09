package br.com.labs.domain.exception;

public sealed class DomainException extends RuntimeException
        permits InvalidCredentialsException,
                UserAlreadyExistsException,
                UserNotFoundException,
                MfaCodeExpiredException,
                MfaCodeInvalidException,
                MfaBlockedException,
                InvalidTokenException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
