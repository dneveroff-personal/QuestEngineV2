package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.TeamRole;
import java.time.Instant;

public record TeamMemberDto(Long id, Long userId, String name, TeamRole role, Instant joinedAt) {}
