package br.com.labs.infrastructure.web.exception;

import br.com.labs.domain.exception.DomainException;
import br.com.labs.domain.exception.InvalidCredentialsException;
import br.com.labs.domain.exception.InvalidTokenException;
import br.com.labs.domain.exception.MfaBlockedException;
import br.com.labs.domain.exception.MfaCodeExpiredException;
import br.com.labs.domain.exception.MfaCodeInvalidException;
import br.com.labs.domain.exception.UserAlreadyExistsException;
import br.com.labs.domain.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://api.loginmfa.com/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        problem.setType(URI.create(TYPE_BASE + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("code", "VALIDATION_001");
        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "invalid-credentials"));
        problem.setTitle("Invalid Credentials");
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "user-already-exists"));
        problem.setTitle("User Already Exists");
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "user-not-found"));
        problem.setTitle("User Not Found");
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(MfaCodeExpiredException.class)
    public ProblemDetail handleMfaCodeExpired(MfaCodeExpiredException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "mfa-code-expired"));
        problem.setTitle("MFA Code Expired");
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(MfaCodeInvalidException.class)
    public ProblemDetail handleMfaCodeInvalid(MfaCodeInvalidException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "mfa-code-invalid"));
        problem.setTitle("Invalid MFA Code");
        problem.setProperty("code", ex.getCode());
        problem.setProperty("remainingAttempts", ex.getRemainingAttempts());
        return problem;
    }

    @ExceptionHandler(MfaBlockedException.class)
    public ProblemDetail handleMfaBlocked(MfaBlockedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "mfa-blocked"));
        problem.setTitle("Too Many Attempts");
        problem.setProperty("code", ex.getCode());
        problem.setProperty("retryAfterSeconds", ex.getBlockedUntilSeconds());
        return problem;
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "invalid-token"));
        problem.setTitle("Invalid Token");
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "bad-request"));
        problem.setTitle("Bad Request");
        problem.setProperty("code", "REQUEST_001");
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("Unhandled domain exception: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "domain-error"));
        problem.setTitle("Domain Error");
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("code", "INTERNAL_001");
        return problem;
    }
}
