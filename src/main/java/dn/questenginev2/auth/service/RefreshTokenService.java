package dn.questenginev2.auth.service;

import dn.questenginev2.auth.entity.RefreshToken;
import dn.questenginev2.auth.repository.RefreshTokenRepository;
import dn.questenginev2.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final Clock clock;

  /** Refresh TTL — default 30 days (ADR-0015). */
  @Value("${jwt.refresh-expiration-days:30}")
  private long refreshExpirationDays;

  /**
   * Issues a new refresh token for the user. Returns the raw token to send to the client; only the
   * SHA-256 hash is stored.
   */
  @Transactional
  public String issue(User user) {
    Instant now = clock.instant();
    String raw = UUID.randomUUID().toString() + "." + UUID.randomUUID();
    RefreshToken entity = new RefreshToken();
    entity.setUser(user);
    entity.setTokenHash(hash(raw));
    entity.setIssuedAt(now);
    entity.setExpiresAt(now.plus(Duration.ofDays(refreshExpirationDays)));
    refreshTokenRepository.save(entity);
    return raw;
  }

  /**
   * Rotates refresh token: validates, revokes old, issues new. On reuse of an already-revoked token
   * — revoke all active tokens for the user (compromise signal).
   */
  @Transactional
  public RotatedTokens rotate(String rawRefreshToken) {
    Instant now = clock.instant();
    String hash = hash(rawRefreshToken);
    RefreshToken existing =
        refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

    if (existing.getRevokedAt() != null) {
      // Reuse detection — revoke all sessions for this user
      refreshTokenRepository.revokeAllActiveForUser(existing.getUser().getId(), now);
      throw new BadCredentialsException("Refresh token reuse detected");
    }

    if (!existing.getExpiresAt().isAfter(now)) {
      existing.setRevokedAt(now);
      refreshTokenRepository.save(existing);
      throw new BadCredentialsException("Refresh token expired");
    }

    existing.setRevokedAt(now);
    String newRaw = issue(existing.getUser());
    RefreshToken replacement =
        refreshTokenRepository
            .findByTokenHash(hash(newRaw))
            .orElseThrow(() -> new IllegalStateException("Just-issued refresh token missing"));
    existing.setReplacedBy(replacement);
    refreshTokenRepository.save(existing);

    return new RotatedTokens(existing.getUser(), newRaw);
  }

  @Transactional
  public void revoke(String rawRefreshToken) {
    Instant now = clock.instant();
    refreshTokenRepository
        .findByTokenHash(hash(rawRefreshToken))
        .ifPresent(
            token -> {
              if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
              }
            });
  }

  @Transactional
  public void revokeAllForUser(Long userId) {
    refreshTokenRepository.revokeAllActiveForUser(userId, clock.instant());
  }

  static String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  public record RotatedTokens(User user, String newRefreshToken) {}
}
