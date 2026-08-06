package dn.questenginev2.team.dto;

import dn.questenginev2.user.entity.User;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record TeamFilterRequest(
    @Size(max = 100) String name, User captain, Instant createdAtAfter, Instant createdAtBefore) {}
