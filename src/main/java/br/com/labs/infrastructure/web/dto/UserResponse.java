package br.com.labs.infrastructure.web.dto;

public record UserResponse(
        String userId,
        String username,
        String email
) {}
