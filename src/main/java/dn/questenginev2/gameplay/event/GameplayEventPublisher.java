package dn.questenginev2.gameplay.event;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes gameplay notification events for STOMP bridge (ADR-022).
 *
 * <p>Callers should invoke this after successful domain writes within the same
 * transaction; the bridge listens with {@code AFTER_COMMIT} so clients never
 * see events for rolled-back state.
 */
@Component
@RequiredArgsConstructor
public class GameplayEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final Clock clock;

  public void publish(
      GameplayEventType type,
      Long questId,
      Long questProgressId,
      Long levelProgressId,
      Map<String, Object> payload) {
    Instant now = clock.instant();
    GameplayEvent event =
        new GameplayEvent(
            type,
            GameplayEvent.CURRENT_VERSION,
            UUID.randomUUID().toString(),
            questId,
            questProgressId,
            levelProgressId,
            now,
            payload == null ? Map.of() : Map.copyOf(payload));
    applicationEventPublisher.publishEvent(new GameplayChangedEvent(this, event));
  }
}
