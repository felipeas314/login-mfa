package br.com.labs.infrastructure.security;

import br.com.labs.domain.auth.SecurityEvent;
import br.com.labs.domain.auth.SecurityEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação do SecurityEventPublisher que loga os eventos.
 *
 * Em produção, poderia:
 * - Enviar para um sistema de SIEM (Splunk, ELK, etc.)
 * - Publicar em uma fila (Kafka, RabbitMQ)
 * - Armazenar em banco para auditoria
 */
@Component
public class LoggingSecurityEventPublisher implements SecurityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Override
    public void publish(SecurityEvent event) {
        switch (event) {
            case SecurityEvent.LoginFailure e ->
                    log.warn("[LOGIN_FAILURE] user={} ip={} reason={}",
                            e.userId(), e.ipAddress(), e.reason());

            case SecurityEvent.MfaFailure e ->
                    log.warn("[MFA_FAILURE] user={} ip={} attempt={}",
                            e.userId(), e.ipAddress(), e.attemptNumber());

            case SecurityEvent.AccountBlocked e ->
                    log.error("[ACCOUNT_BLOCKED] user={} ip={} reason={} duration={}s",
                            e.userId(), e.ipAddress(), e.reason(), e.blockedForSeconds());

            case SecurityEvent.SuspiciousActivity e ->
                    log.warn("[SUSPICIOUS_ACTIVITY] severity={} user={} ip={} description={}",
                            e.severity(), e.userId(), e.ipAddress(), e.description());
        }
    }
}
