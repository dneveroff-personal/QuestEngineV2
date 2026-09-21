package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.TeamRole;
import java.time.Instant;

/**
 * Member of a team.
 *
 * <p>{@code username} — stable identity (login / JWT {@code sub}).
 * {@code displayName} — UI label: {@code publicName} if set, otherwise {@code username}.
 */
public record TeamMemberDto(
    Long id, Long userId, String username, String displayName, TeamRole role, Instant joinedAt) {}
