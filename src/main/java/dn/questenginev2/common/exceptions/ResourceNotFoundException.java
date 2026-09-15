package dn.questenginev2.common.exceptions;

/**
 * ADR-0011: сущность с данным id не существует — 404 Not Found. Общий
 * заменитель мест, где ранее использовался {@code IllegalArgumentException}
 * для этого смысла. Не заменяет уже существующие специфичные исключения
 * ({@code UserNotFoundException}, {@code TeamNotFoundException} и т.п.) —
 * те и так корректно маппятся на 404, трогать их не нужно.
 */
public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
