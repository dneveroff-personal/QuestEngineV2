package dn.questenginev2.team.dto;

import java.time.Instant;
import java.util.List;

public record TeamResponse(
    Long id, String name, String captainName, Instant createdAt, List<TeamMemberDto> members) {}
