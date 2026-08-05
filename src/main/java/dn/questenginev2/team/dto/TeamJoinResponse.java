package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.JoinRequestType;
import java.time.Instant;

public record TeamJoinResponse(
    Long requestId, String userName, JoinRequestType type, Instant createdAt) {}
