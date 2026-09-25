package dn.questenginev2.gameplay.event;

import java.time.Instant;
import java.util.Map;

/**
 * STOMP payload envelope for gameplay realtime (docs/frontend/realtime.md §4.3).
 *
 * <p>{@code eventId} is used by the frontend for dedupe after reconnect.
 */
public record GameplayEvent(
    GameplayEventType type,
    int version,
    String eventId,
    Long questId,
    Long questProgressId,
    Long levelProgressId,
    Instant occurredAt,
    Map<String, Object> payload) {

  public static final int CURRENT_VERSION = 1;
}
