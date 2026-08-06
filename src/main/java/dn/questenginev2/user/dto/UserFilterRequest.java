package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UserFilterRequest(
    @Size(max = 64) String username,
    @Size(max = 128) String email,
    UserRole role,
    Instant createdAtAfter,
    Instant createdAtBefore) {}
