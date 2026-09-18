package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import java.time.Instant;

/**
 * User projection for API.
 *
 * <p>For {@code GET /api/users/search}: non-ADMIN callers receive only {@code id}, {@code
 * username}, {@code publicName}; {@code email}, {@code role}, {@code createdAt} are null (see
 * threat-model / backlog Auth #6).
 */
public record UserResponse(
    Long id, String username, String publicName, String email, UserRole role, Instant createdAt) {}
