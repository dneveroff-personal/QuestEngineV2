/**
 * Backend отдаёт `Instant` как ISO-строку (UTC) — форматируем в локальное
 * время пользователя. Один helper, чтобы формат не расходился по экранам.
 */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
