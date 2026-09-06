import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import type { CodeSubmissionResponse } from "@/api/gameplay";
import { CodeSubmitForm } from "@/features/gameplay/CodeSubmitForm";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";

function mockSubmitCode(response: CodeSubmissionResponse) {
  server.use(
    http.post("/api/quests/progress/:questId/:teamId/codes", () => HttpResponse.json(response)),
  );
}

async function submit(code: string) {
  const user = userEvent.setup();
  await user.type(screen.getByPlaceholderText("Код"), code);
  await user.click(screen.getByRole("button", { name: "Отправить" }));
}

describe("CodeSubmitForm", () => {
  it("CORRECT_MAIN — показывает «Верно!» и остаток кодов", async () => {
    mockSubmitCode({
      result: "CORRECT_MAIN",
      remainingMainCodes: 2,
      levelCompleted: false,
      questFinished: false,
      submittedAt: "2026-02-01T10:00:00Z",
    });
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    await submit("MAIN01");

    expect(await screen.findByText("Верно!")).toBeInTheDocument();
    expect(screen.getByText("Осталось основных кодов: 2")).toBeInTheDocument();
    // Поле очищается после успешной отправки.
    expect(screen.getByPlaceholderText("Код")).toHaveValue("");
  });

  it("INCORRECT — показывает «Неверный код.»", async () => {
    mockSubmitCode({
      result: "INCORRECT",
      remainingMainCodes: null,
      levelCompleted: false,
      questFinished: false,
      submittedAt: "2026-02-01T10:00:00Z",
    });
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    await submit("WRONG");

    expect(await screen.findByText("Неверный код.")).toBeInTheDocument();
    // remainingMainCodes: null — строку с остатком не показываем вовсе.
    expect(screen.queryByText(/Осталось основных кодов/)).not.toBeInTheDocument();
  });

  it("levelCompleted без questFinished — «Уровень пройден»", async () => {
    mockSubmitCode({
      result: "CORRECT_MAIN",
      remainingMainCodes: 0,
      levelCompleted: true,
      questFinished: false,
      submittedAt: "2026-02-01T10:00:00Z",
    });
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    await submit("LAST01");

    expect(await screen.findByText("Уровень пройден, переходим дальше.")).toBeInTheDocument();
    expect(screen.queryByText(/Квест завершён/)).not.toBeInTheDocument();
  });

  it("levelCompleted + questFinished — «Квест завершён»", async () => {
    mockSubmitCode({
      result: "CORRECT_MAIN",
      remainingMainCodes: 0,
      levelCompleted: true,
      questFinished: true,
      submittedAt: "2026-02-01T10:00:00Z",
    });
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    await submit("FINAL01");

    expect(await screen.findByText("Квест завершён! 🎉")).toBeInTheDocument();
  });

  it("ошибка backend показывается пользователю", async () => {
    server.use(
      http.post("/api/quests/progress/:questId/:teamId/codes", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Conflict",
            status: 409,
            detail: "Такой код уже был отправлен вашей командой.",
          },
          { status: 409 },
        );
      }),
    );
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    await submit("DUP01");

    expect(
      await screen.findByText("Такой код уже был отправлен вашей командой."),
    ).toBeInTheDocument();
  });

  it("пустой ввод — кнопка отключена, запрос не уходит", async () => {
    // Намеренно не регистрируем handler — onUnhandledRequest: "error"
    // (setup.ts) провалит тест, если компонент всё же отправит запрос.
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    const button = screen.getByRole("button", { name: "Отправить" });
    expect(button).toBeDisabled();

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText("Код"), "   ");
    expect(button).toBeDisabled();
  });

  it("во время ожидания ответа кнопка показывает «Проверяем...» и отключена", async () => {
    server.use(
      http.post("/api/quests/progress/:questId/:teamId/codes", async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
        return HttpResponse.json({
          result: "CORRECT_MAIN",
          remainingMainCodes: 1,
          levelCompleted: false,
          questFinished: false,
          submittedAt: "2026-02-01T10:00:00Z",
        } satisfies CodeSubmissionResponse);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<CodeSubmitForm questId={1} teamId={1} />);

    await user.type(screen.getByPlaceholderText("Код"), "SLOW01");
    await user.click(screen.getByRole("button", { name: "Отправить" }));

    expect(screen.getByRole("button", { name: "Проверяем..." })).toBeDisabled();
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Отправить" })).toBeInTheDocument();
    });
  });
});
