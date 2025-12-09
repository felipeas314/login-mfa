package br.com.labs.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyMfaRequest(
        @NotBlank(message = "MFA token is required")
        String mfaToken,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Code must be exactly 6 digits")
        String code
) {}
