package dn.questenginev2.common.exceptions;

/**
 * ADR-0011: у пользователя ЕСТЬ права на действие, но текущее состояние
 * ресурса не позволяет его выполнить (неверный статус, лимит достигнут,
 * просрочено и т.п.) — 409 Conflict. Не путать с {@link
 * ForbiddenOperationException} (403 — прав нет в принципе).
 */
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
