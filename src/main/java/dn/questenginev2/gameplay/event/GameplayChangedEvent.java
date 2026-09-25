package dn.questenginev2.gameplay.event;

import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published after a gameplay state change.
 * Bridged to STOMP only after successful transaction commit.
 */
public class GameplayChangedEvent extends ApplicationEvent {

  private final GameplayEvent payload;

  public GameplayChangedEvent(Object source, GameplayEvent payload) {
    super(source);
    this.payload = payload;
  }

  public GameplayEvent getPayload() {
    return payload;
  }
}
