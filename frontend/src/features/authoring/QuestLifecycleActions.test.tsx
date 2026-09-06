import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { useLocation } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Quest } from "@/api/quests";
import { QuestLifecycleActions } from "@/features/authoring/QuestLifecycleActions";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";

const DRAFT_QUEST: Quest = {
  id: 7,
  title: "Черновик квеста",
  description: "",
  type: "TEAM",
  status: "DRAFT",
  createdAt: "2026-01-01T00:00:00Z",
  startTime: "2026-02-01T10:00:00Z",
  finishTime: "2026-02-01T18:00:00Z",
};

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname}</div>;
}

function renderActions(quest: Quest) {
  return renderWithProviders(
    <>
      <QuestLifecycleActions quest={quest} />
      <LocationProbe />
    </>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("QuestLifecycleActions", () => {
  it("DRAFT: показывает «Опубликовать», без «Завершить»", () => {
    renderActions(DRAFT_QUEST);

    expect(screen.getByRole("button", { name: "Опубликовать" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Завершить" })).not.toBeInTheDocument();
  });

  it("RUNNING: показывает «Завершить», без «Опубликовать»", () => {
    renderActions({ ...DRAFT_QUEST, status: "RUNNING" });

    expect(screen.getByRole("button", { name: "Завершить" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Опубликовать" })).not.toBeInTheDocument();
  });

  it("публикация: ошибка backend (например, аномальный уровень) показывается как есть", async () => {
    server.use(
      http.post("/api/quests/:questId/publish", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Forbidden",
            status: 403,
            detail: "У уровня «Уровень 1» нет ни кодов, ни автоперехода.",
          },
          { status: 403 },
        );
      }),
    );
    const user = userEvent.setup();
    renderActions(DRAFT_QUEST);

    await user.click(screen.getByRole("button", { name: "Опубликовать" }));

    expect(
      await screen.findByText("У уровня «Уровень 1» нет ни кодов, ни автоперехода."),
    ).toBeInTheDocument();
  });

  it("DRAFT: удаление через window.confirm — подтверждение переходит на /author", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    server.use(http.delete("/api/quests/:questId", () => new HttpResponse(null, { status: 204 })));
    const user = userEvent.setup();
    renderActions(DRAFT_QUEST);

    await user.click(screen.getByRole("button", { name: "Удалить" }));

    await waitFor(() => {
      expect(screen.getByTestId("location-probe")).toHaveTextContent("/author");
    });
  });

  it("DRAFT: отмена в window.confirm — запрос не уходит, остаёмся на месте", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    // Намеренно НЕ регистрируем handler на DELETE — если бы компонент всё
    // же его вызвал, onUnhandledRequest: "error" (setup.ts) провалил бы тест.
    const user = userEvent.setup();
    renderActions(DRAFT_QUEST);

    await user.click(screen.getByRole("button", { name: "Удалить" }));

    expect(screen.getByTestId("location-probe")).toHaveTextContent("/");
  });

  it("RUNNING: удаление НЕ использует window.confirm — требует набрать точное название квеста", async () => {
    const confirmSpy = vi.spyOn(window, "confirm");
    server.use(http.delete("/api/quests/:questId", () => new HttpResponse(null, { status: 204 })));
    const user = userEvent.setup();
    const runningQuest = { ...DRAFT_QUEST, status: "RUNNING" as const };
    renderActions(runningQuest);

    await user.click(screen.getByRole("button", { name: "Удалить" }));

    // window.confirm НЕ должен вызываться для высокорискового пути.
    expect(confirmSpy).not.toHaveBeenCalled();

    const deleteButton = screen.getByRole("button", { name: "Удалить безвозвратно" });
    expect(deleteButton).toBeDisabled();

    const input = screen.getByRole("textbox");
    await user.type(input, "неверное название");
    expect(deleteButton).toBeDisabled();

    await user.clear(input);
    await user.type(input, runningQuest.title);
    expect(deleteButton).not.toBeDisabled();

    await user.click(deleteButton);

    await waitFor(() => {
      expect(screen.getByTestId("location-probe")).toHaveTextContent("/author");
    });
  });

  it("RUNNING: кнопка «Отмена» в подтверждении возвращает к обычному виду", async () => {
    const user = userEvent.setup();
    renderActions({ ...DRAFT_QUEST, status: "RUNNING" });

    await user.click(screen.getByRole("button", { name: "Удалить" }));
    expect(screen.getByRole("button", { name: "Удалить безвозвратно" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Отмена" }));

    expect(screen.queryByRole("button", { name: "Удалить безвозвратно" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Завершить" })).toBeInTheDocument();
  });
});
