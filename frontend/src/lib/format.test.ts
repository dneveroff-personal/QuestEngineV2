import { describe, expect, it } from "vitest";

import { formatDateTime } from "@/lib/format";

describe("formatDateTime", () => {
  it("возвращает «не указано» для null, а не молчаливую эпоху 1970 года", () => {
    // Регрессия: new Date(null) в JS не бросает исключение, а тихо
    // возвращает 1 января 1970 — выглядит как настоящая дата. Quest.startTime
    // (api/quests.ts) реально может быть null (QuestResponse.java без
    // @NotNull, наша же QuestForm.tsx отправляет null для незаполненного поля).
    expect(formatDateTime(null)).toBe("не указано");
  });

  it("форматирует валидную ISO-строку без исключений", () => {
    const result = formatDateTime("2026-02-01T10:00:00Z");

    expect(result).not.toBe("не указано");
    expect(result).toContain("2026");
  });
});
