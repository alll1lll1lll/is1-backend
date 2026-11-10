package ru.itmo.is1.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.is1.dto.auth.AuthResponse;
import ru.itmo.is1.dto.auth.LoginRequest;
import ru.itmo.is1.dto.auth.RegisterRequest;
import ru.itmo.is1.exception.InvalidRefreshTokenException;
import ru.itmo.is1.exception.UsernameTakenException;
import ru.itmo.is1.model.AppUser;
import ru.itmo.is1.repository.AppUserRepository;
import ru.itmo.is1.security.IssuedToken;
import ru.itmo.is1.security.JwtService;
import ru.itmo.is1.security.RefreshTokenService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        AppUser user = userRepository.saveAndFlush(
                new AppUser(username, passwordEncoder.encode(request.getPassword())));
        logAuthEvent("user_registered", username);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (AuthenticationException e) {
            log.atWarn()
                    .setMessage("Login failed")
                    .addKeyValue("event", "login_failed")
                    .addKeyValue("username", username)
                    .addKeyValue("reason", e.getClass().getSimpleName())
                    .log();
            throw e;
        }
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        logAuthEvent("login_succeeded", username);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(String refreshToken) {
        AppUser user = refreshTokenService.consume(refreshToken);
        logAuthEvent("tokens_refreshed", user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken)
                .ifPresent(username -> logAuthEvent("logged_out", username));
    }

    private static void logAuthEvent(String event, String username) {
        log.atInfo()
                .setMessage("Authentication event")
                .addKeyValue("event", event)
                .addKeyValue("username", username)
                .log();
    }

    private AuthResponse issueTokens(AppUser user) {
        IssuedToken accessToken = jwtService.issueAccessToken(user.getUsername());
        IssuedToken refreshToken = refreshTokenService.issue(user);
        return AuthResponse.builder()
                .tokenType(TOKEN_TYPE)
                .accessToken(accessToken.getValue())
                .accessTokenExpiresAt(accessToken.getExpiresAt())
                .refreshToken(refreshToken.getValue())
                .refreshTokenExpiresAt(refreshToken.getExpiresAt())
                .username(user.getUsername())
                .build();
    }
}
