package br.com.labs.infrastructure.web.controller;

import br.com.labs.application.usecase.AuthenticateUserUseCase;
import br.com.labs.application.usecase.LogoutUseCase;
import br.com.labs.application.usecase.RefreshTokenUseCase;
import br.com.labs.application.usecase.RegisterUserUseCase;
import br.com.labs.application.usecase.VerifyMfaCodeUseCase;
import br.com.labs.infrastructure.web.dto.LoginRequest;
import br.com.labs.infrastructure.web.dto.LogoutRequest;
import br.com.labs.infrastructure.web.dto.MfaResponse;
import br.com.labs.infrastructure.web.dto.RefreshTokenRequest;
import br.com.labs.infrastructure.web.dto.RegisterRequest;
import br.com.labs.infrastructure.web.dto.TokenResponse;
import br.com.labs.infrastructure.web.dto.UserResponse;
import br.com.labs.infrastructure.web.dto.VerifyMfaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final VerifyMfaCodeUseCase verifyMfaCodeUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            AuthenticateUserUseCase authenticateUserUseCase,
            VerifyMfaCodeUseCase verifyMfaCodeUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.verifyMfaCodeUseCase = verifyMfaCodeUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var input = new RegisterUserUseCase.Input(
                request.username(),
                request.email(),
                request.password()
        );

        var output = registerUserUseCase.execute(input);

        var response = new UserResponse(output.userId(), output.username(), output.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<MfaResponse> login(@Valid @RequestBody LoginRequest request) {
        var input = new AuthenticateUserUseCase.Input(request.username(), request.password());

        var output = authenticateUserUseCase.execute(input);

        var response = new MfaResponse(output.mfaToken(), output.expiresIn());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<TokenResponse> verify(@Valid @RequestBody VerifyMfaRequest request) {
        var input = new VerifyMfaCodeUseCase.Input(request.mfaToken(), request.code());

        var output = verifyMfaCodeUseCase.execute(input);

        var response = new TokenResponse(
                output.accessToken(),
                output.refreshToken(),
                output.accessTokenExpiresIn(),
                output.refreshTokenExpiresIn()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var input = new RefreshTokenUseCase.Input(request.refreshToken());

        var output = refreshTokenUseCase.execute(input);

        var response = new TokenResponse(
                output.accessToken(),
                output.refreshToken(),
                output.accessTokenExpiresIn(),
                output.refreshTokenExpiresIn()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request
    ) {
        String accessToken = extractToken(authorizationHeader);
        String refreshToken = request != null ? request.refreshToken() : null;

        var input = new LogoutUseCase.Input(accessToken, refreshToken);
        logoutUseCase.execute(input);

        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
