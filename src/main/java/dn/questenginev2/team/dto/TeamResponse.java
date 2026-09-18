package dn.questenginev2.team.dto;

import java.time.Instant;
import java.util.List;

/**
 * Team projection.
 *
 * <p>{@code captainUsername} — stable identity; {@code captainDisplayName} — UI label
 * ({@code publicName} or fallback to username).
 */
public record TeamResponse(
    Long id,
    String name,
    String captainUsername,
    String captainDisplayName,
    Instant createdAt,
    List<TeamMemberDto> members) {}
