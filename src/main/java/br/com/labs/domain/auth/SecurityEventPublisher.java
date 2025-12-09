package br.com.labs.domain.auth;

public interface SecurityEventPublisher {

    void publish(SecurityEvent event);
}
