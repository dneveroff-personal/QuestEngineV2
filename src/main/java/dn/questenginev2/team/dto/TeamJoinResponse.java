package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.JoinRequestType;
import java.time.Instant;

/**
 * Join request or captain invite.
 *
 * <ul>
 *   <li>{@code JOIN_REQUEST} — user applied to join a team (shown to captain): {@code userName} is
 *       the applicant.
 *   <li>{@code CAPTAIN_INVITE} — captain invited a user (shown to invitee): {@code teamName} /
 *       {@code teamId} identify the inviting team; {@code userName} is the invitee.
 * </ul>
 */
public record TeamJoinResponse(
    Long requestId,
    Long teamId,
    String teamName,
    String userName,
    JoinRequestType type,
    Instant createdAt) {}
