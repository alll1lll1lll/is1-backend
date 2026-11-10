package ru.itmo.is1.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.itmo.is1.exception.InvalidRefreshTokenException;
import ru.itmo.is1.model.AppUser;
import ru.itmo.is1.model.RefreshToken;
import ru.itmo.is1.repository.RefreshTokenRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties properties;

    @Transactional
    public IssuedToken issue(AppUser user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(properties.getRefreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), Date.from(expiresAt)));
        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AppUser consume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Date now = new Date();
        if (token.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllActiveByUserId(token.getUser().getId(), now);
            log.atWarn()
                    .setMessage("Revoked refresh token reused, all user sessions revoked")
                    .addKeyValue("event", "refresh_token_reuse")
                    .addKeyValue("username", token.getUser().getUsername())
                    .addKeyValue("revokedTokens", revoked)
                    .log();
            throw new InvalidRefreshTokenException();
        }
        if (token.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke(now);
        return token.getUser();
    }

    @Transactional
    public Optional<String> revoke(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .map(token -> {
                    token.revoke(new Date());
                    return token.getUser().getUsername();
                });
    }

    @Transactional
    @Scheduled(cron = "${app.jwt.cleanup-cron}")
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(new Date());
        log.atInfo()
                .setMessage("Expired refresh tokens deleted")
                .addKeyValue("event", "refresh_token_cleanup")
                .addKeyValue("deletedTokens", deleted)
                .log();
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
