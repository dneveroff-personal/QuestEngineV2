package dn.questenginev2.gameplay.event;

/**
 * Server → client gameplay notification types (ADR-022, docs/frontend/realtime.md).
 *
 * <p>Events are notifications only; REST remains the source of truth.
 */
public enum GameplayEventType {
  CODE_ACCEPTED,
  CODE_REJECTED,
  LEVEL_COMPLETED,
  LEVEL_AUTO_TRANSITIONED,
  HINT_REVEALED,
  QUEST_FINISHED
}
