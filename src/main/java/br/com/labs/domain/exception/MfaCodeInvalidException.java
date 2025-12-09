package br.com.labs.domain.exception;

public final class MfaCodeInvalidException extends DomainException {

    private final int remainingAttempts;

    public MfaCodeInvalidException(int remainingAttempts) {
        super("MFA_002", "Invalid MFA code. %d attempts remaining".formatted(remainingAttempts));
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
