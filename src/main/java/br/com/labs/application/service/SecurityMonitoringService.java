package br.com.labs.application.service;

import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.SecurityEvent;
import br.com.labs.domain.auth.SecurityEvent.SeverityLevel;
import br.com.labs.domain.auth.SecurityEventPublisher;
import br.com.labs.domain.user.UserId;
import br.com.labs.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SecurityMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SecurityMonitoringService.class);

    private final MfaRepository mfaRepository;
    private final UserRepository userRepository;
    private final SecurityEventPublisher eventPublisher;
    private final int maxLoginFailures;
    private final int suspiciousThreshold;

    public SecurityMonitoringService(
            MfaRepository mfaRepository,
            UserRepository userRepository,
            SecurityEventPublisher eventPublisher,
            @Value("${security.max-login-failures:5}") int maxLoginFailures,
            @Value("${security.suspicious-threshold:3}") int suspiciousThreshold
    ) {
        this.mfaRepository = mfaRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.maxLoginFailures = maxLoginFailures;
        this.suspiciousThreshold = suspiciousThreshold;
    }

    /**
     * Registra falha de login e toma ações se necessário.
     * Chamado pelo AuthenticateUserUseCase quando credenciais são inválidas.
     */
    public void recordLoginFailure(UserId userId, String ipAddress, String reason) {
        var event = new SecurityEvent.LoginFailure(userId, Instant.now(), ipAddress, reason);
        eventPublisher.publish(event);

        int failures = mfaRepository.incrementAttempts(userId);
        log.warn("Login failure #{} for user {} from IP {}", failures, userId, ipAddress);

        if (failures >= maxLoginFailures) {
            blockAccount(userId, ipAddress, "Too many failed login attempts");
        } else if (failures >= suspiciousThreshold) {
            reportSuspiciousActivity(userId, ipAddress,
                    "Multiple login failures: " + failures, SeverityLevel.MEDIUM);
        }
    }

    /**
     * Registra falha de MFA e verifica padrões suspeitos.
     * Chamado pelo VerifyMfaCodeUseCase quando código é inválido.
     */
    public void recordMfaFailure(UserId userId, String ipAddress, int attemptNumber) {
        var event = new SecurityEvent.MfaFailure(userId, Instant.now(), ipAddress, attemptNumber);
        eventPublisher.publish(event);

        log.warn("MFA failure #{} for user {} from IP {}", attemptNumber, userId, ipAddress);

        if (attemptNumber >= suspiciousThreshold) {
            reportSuspiciousActivity(userId, ipAddress,
                    "Multiple MFA failures may indicate code interception attempt",
                    SeverityLevel.HIGH);
        }
    }

    /**
     * Bloqueia conta do usuário e invalida todos os tokens.
     * Ação severa tomada em caso de atividade suspeita confirmada.
     */
    public void blockAccount(UserId userId, String ipAddress, String reason) {
        mfaRepository.block(userId);
        long blockTtl = mfaRepository.getBlockTtl(userId);

        var event = new SecurityEvent.AccountBlocked(
                userId, Instant.now(), ipAddress, reason, blockTtl
        );
        eventPublisher.publish(event);

        log.error("Account blocked for user {} from IP {}. Reason: {}", userId, ipAddress, reason);

        notifyUserAboutBlock(userId, reason, blockTtl);
    }

    /**
     * Invalida todas as sessões do usuário.
     * Útil quando detectamos comprometimento de conta.
     */
    public void invalidateAllSessions(UserId userId, String reason) {
        log.warn("Invalidating all sessions for user {}. Reason: {}", userId, reason);

        reportSuspiciousActivity(userId, "system",
                "All sessions invalidated: " + reason, SeverityLevel.CRITICAL);
    }

    /**
     * Reporta atividade suspeita para análise.
     */
    public void reportSuspiciousActivity(
            UserId userId,
            String ipAddress,
            String description,
            SeverityLevel severity
    ) {
        var event = new SecurityEvent.SuspiciousActivity(
                userId, Instant.now(), ipAddress, description, severity
        );
        eventPublisher.publish(event);

        log.warn("Suspicious activity [{}] for user {} from IP {}: {}",
                severity, userId, ipAddress, description);

        if (severity == SeverityLevel.HIGH || severity == SeverityLevel.CRITICAL) {
            notifyUserAboutSuspiciousActivity(userId, description);
        }
    }

    /**
     * Reseta contadores de falha após login bem-sucedido.
     * Chamado pelo VerifyMfaCodeUseCase após verificação bem-sucedida.
     */
    public void recordSuccessfulLogin(UserId userId, String ipAddress) {
        mfaRepository.deleteCode(userId);
        log.info("Successful login for user {} from IP {}", userId, ipAddress);
    }

    private void notifyUserAboutBlock(UserId userId, String reason, long blockSeconds) {
        userRepository.findById(userId).ifPresent(user -> {
            log.info("Sending block notification to user {}", userId);
            // Em produção, usaria um template específico
            // emailSender.sendSecurityAlert(user.getEmail(), "Account Blocked", reason);
        });
    }

    private void notifyUserAboutSuspiciousActivity(UserId userId, String description) {
        userRepository.findById(userId).ifPresent(user -> {
            log.info("Sending suspicious activity notification to user {}", userId);
            // Em produção, usaria um template específico
            // emailSender.sendSecurityAlert(user.getEmail(), "Suspicious Activity", description);
        });
    }
}
