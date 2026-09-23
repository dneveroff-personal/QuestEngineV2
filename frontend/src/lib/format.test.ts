import { describe, expect, it } from "vitest";

import { formatDateTime, formatDurationSeconds } from "@/lib/format";

describe("formatDateTime", () => {
  it("возвращает «не указано» для null, а не молчаливую эпоху 1970 года", () => {
    expect(formatDateTime(null)).toBe("не указано");
  });

  it("форматирует валидную ISO-строку без исключений", () => {
    const result = formatDateTime("2026-02-01T10:00:00Z");

    expect(result).not.toBe("не указано");
    expect(result).toContain("2026");
  });
});

describe("formatDurationSeconds", () => {
  it("null → em dash", () => {
    expect(formatDurationSeconds(null)).toBe("—");
    expect(formatDurationSeconds(undefined)).toBe("—");
  });

  it("formats minutes and seconds", () => {
    expect(formatDurationSeconds(65)).toBe("1:05");
  });

  it("formats hours", () => {
    expect(formatDurationSeconds(3661)).toBe("1:01:01");
  });
});
