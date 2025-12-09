package br.com.labs.domain.exception;

public final class MfaCodeExpiredException extends DomainException {

    public MfaCodeExpiredException() {
        super("MFA_001", "MFA code has expired");
    }
}
