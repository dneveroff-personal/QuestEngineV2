package dn.questenginev2.gameplay.ws;

import dn.questenginev2.gameplay.event.GameplayChangedEvent;
import dn.questenginev2.gameplay.event.GameplayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges domain gameplay events to STOMP destinations after commit (ADR-022).
 *
 * <p>Team-scoped events go to {@code /topic/quest-progress/{id}/gameplay}.
 * Quest-wide destination is reserved for future slices that need it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameplayStompBridge {

  private final SimpMessagingTemplate messagingTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGameplayChanged(GameplayChangedEvent applicationEvent) {
    GameplayEvent event = applicationEvent.getPayload();
    if (event.questProgressId() == null) {
      log.warn("Skipping STOMP publish: missing questProgressId for {}", event.type());
      return;
    }
    String destination = "/topic/quest-progress/" + event.questProgressId() + "/gameplay";
    messagingTemplate.convertAndSend(destination, event);
    log.debug("Published {} to {}", event.type(), destination);
  }
}
