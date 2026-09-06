/**
 * Backend отдаёт `Instant` как ISO-строку (UTC) — форматируем в локальное
 * время пользователя. Один helper, чтобы формат не расходился по экранам.
 *
 * Принимает `null` явно: `Quest.startTime`/`finishTime` (QuestResponse.java)
 * — обычный `Instant` без `@NotNull`, и наша же QuestForm.tsx отправляет
 * `null`, если поле не заполнено при создании квеста. `new Date(null)` в
 * JS не бросает исключение — тихо возвращает 1 января 1970, что хуже
 * явной ошибки (выглядит как настоящая, но неверная дата).
 */
export function formatDateTime(iso: string | null): string {
  if (!iso) return "не указано";

  return new Date(iso).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
