package br.com.labs.infrastructure.security;

import br.com.labs.domain.auth.TokenRepository;
import br.com.labs.domain.exception.InvalidTokenException;
import br.com.labs.domain.user.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenRepository tokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var token = extractToken(request);

        if (token != null) {
            try {
                var jti = jwtTokenProvider.extractJti(token);

                if (tokenRepository.isBlacklisted(jti)) {
                    throw new InvalidTokenException("Token revoked");
                }

                UserId userId = jwtTokenProvider.validateAccessToken(token);
                setAuthentication(userId, request);

            } catch (InvalidTokenException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private void setAuthentication(UserId userId, HttpServletRequest request) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
