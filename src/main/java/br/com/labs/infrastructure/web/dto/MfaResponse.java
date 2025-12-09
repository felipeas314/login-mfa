package br.com.labs.infrastructure.web.dto;

public record MfaResponse(
        String mfaToken,
        long expiresIn
) {}
