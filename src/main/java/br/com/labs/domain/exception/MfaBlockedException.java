package br.com.labs.domain.exception;

public final class MfaBlockedException extends DomainException {

    private final long blockedUntilSeconds;

    public MfaBlockedException(long blockedUntilSeconds) {
        super("MFA_003", "Too many failed attempts. Try again in %d seconds".formatted(blockedUntilSeconds));
        this.blockedUntilSeconds = blockedUntilSeconds;
    }

    public long getBlockedUntilSeconds() {
        return blockedUntilSeconds;
    }
}
