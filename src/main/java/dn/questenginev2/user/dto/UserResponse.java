package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import java.time.Instant;

public record UserResponse(
    Long id, String publicName, String email, UserRole role, Instant createdAt) {}
