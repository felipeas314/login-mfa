package br.com.labs.domain.auth;

import br.com.labs.domain.user.UserId;

import java.time.Instant;

public sealed interface SecurityEvent {

    UserId userId();
    Instant occurredAt();
    String ipAddress();

    record LoginFailure(
            UserId userId,
            Instant occurredAt,
            String ipAddress,
            String reason
    ) implements SecurityEvent {}

    record MfaFailure(
            UserId userId,
            Instant occurredAt,
            String ipAddress,
            int attemptNumber
    ) implements SecurityEvent {}

    record AccountBlocked(
            UserId userId,
            Instant occurredAt,
            String ipAddress,
            String reason,
            long blockedForSeconds
    ) implements SecurityEvent {}

    record SuspiciousActivity(
            UserId userId,
            Instant occurredAt,
            String ipAddress,
            String description,
            SeverityLevel severity
    ) implements SecurityEvent {}

    enum SeverityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
