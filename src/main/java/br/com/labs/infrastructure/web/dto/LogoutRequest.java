package br.com.labs.infrastructure.web.dto;

public record LogoutRequest(
        String refreshToken
) {}
