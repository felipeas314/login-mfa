package br.com.labs.infrastructure.web.controller;

import br.com.labs.domain.auth.MfaRepository;
import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.user.UserId;
import br.com.labs.infrastructure.IntegrationTestBase;
import br.com.labs.infrastructure.security.JwtTokenProvider;
import br.com.labs.infrastructure.web.dto.LoginRequest;
import br.com.labs.infrastructure.web.dto.RefreshTokenRequest;
import br.com.labs.infrastructure.web.dto.RegisterRequest;
import br.com.labs.infrastructure.web.dto.VerifyMfaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MfaRepository mfaRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUser() throws Exception {
        var request = new RegisterRequest("newuser", "newuser@test.com", "Password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.email", is("newuser@test.com")));
    }

    @Test
    @DisplayName("Should return conflict when username exists")
    void shouldReturnConflictWhenUsernameExists() throws Exception {
        var request1 = new RegisterRequest("duplicate", "duplicate1@test.com", "Password123");
        var request2 = new RegisterRequest("duplicate", "duplicate2@test.com", "Password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("USER_001")));
    }

    @Test
    @DisplayName("Should return validation error for invalid request")
    void shouldReturnValidationError() throws Exception {
        var request = new RegisterRequest("ab", "invalid-email", "weak");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_001")))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("Should login and receive MFA token")
    void shouldLoginAndReceiveMfaToken() throws Exception {
        var registerRequest = new RegisterRequest("loginuser", "loginuser@test.com", "Password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var loginRequest = new LoginRequest("loginuser", "Password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaToken", notNullValue()))
                .andExpect(jsonPath("$.expiresIn", notNullValue()));
    }

    @Test
    @DisplayName("Should return unauthorized for invalid credentials")
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        var loginRequest = new LoginRequest("nonexistent", "Password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    @Test
    @DisplayName("Should verify MFA code and return tokens")
    void shouldVerifyMfaCodeAndReturnTokens() throws Exception {
        var registerRequest = new RegisterRequest("mfauser", "mfauser@test.com", "Password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var loginRequest = new LoginRequest("mfauser", "Password123");
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        var mfaToken = loginResponse.get("mfaToken").asText();

        var userId = jwtTokenProvider.validateMfaToken(mfaToken);
        var storedCode = mfaRepository.findCode(userId).orElseThrow();

        var verifyRequest = new VerifyMfaRequest(mfaToken, storedCode.value());
        mockMvc.perform(post("/api/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("Should refresh tokens")
    void shouldRefreshTokens() throws Exception {
        var userId = UserId.generate();
        var tokenPair = jwtTokenProvider.generateTokenPair(userId);
        var refreshTokenId = jwtTokenProvider.extractRefreshTokenId(tokenPair.refreshToken());
        tokenRepository.saveRefreshToken(refreshTokenId, userId);

        var refreshRequest = new RefreshTokenRequest(tokenPair.refreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        var userId = UserId.generate();
        var tokenPair = jwtTokenProvider.generateTokenPair(userId);
        var refreshTokenId = jwtTokenProvider.extractRefreshTokenId(tokenPair.refreshToken());
        tokenRepository.saveRefreshToken(refreshTokenId, userId);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + tokenPair.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + tokenPair.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());
    }
}
