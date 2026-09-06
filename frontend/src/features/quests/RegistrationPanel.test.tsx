import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import type { Quest, QuestRegistration } from "@/api/quests";
import type { Team } from "@/api/teams";
import { RegistrationPanel } from "@/features/quests/RegistrationPanel";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";

const BASE_QUEST: Quest = {
  id: 1,
  title: "Тестовый квест",
  description: "Описание",
  type: "TEAM",
  status: "REGISTRATION",
  createdAt: "2026-01-01T00:00:00Z",
  startTime: "2026-02-01T10:00:00Z",
  finishTime: "2026-02-01T18:00:00Z",
};

const MY_TEAM: Team = {
  id: 42,
  name: "Котики",
  captainName: "captain_user",
  createdAt: "2026-01-01T00:00:00Z",
  members: [],
};

function mockMyTeam(team: Team | null) {
  server.use(
    http.get("/api/teams/my", () => {
      if (!team) {
        return HttpResponse.json(
          { type: "about:blank", title: "Not Found", status: 404, detail: "Команда не найдена" },
          { status: 404 },
        );
      }
      return HttpResponse.json(team);
    }),
  );
}

describe("RegistrationPanel", () => {
  it("без команды показывает предложение создать/вступить, не показывает статус заявки", async () => {
    mockMyTeam(null);
    renderWithProviders(<RegistrationPanel quest={BASE_QUEST} registrations={[]} />);

    expect(await screen.findByText(/нужна команда/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /создать или вступить/i })).toHaveAttribute(
      "href",
      "/team",
    );
  });

  it("с командой, без заявки, квест в REGISTRATION — показывает кнопку подачи заявки", async () => {
    mockMyTeam(MY_TEAM);
    server.use(
      http.post("/api/quests/register/:questId/:teamId", () => {
        return HttpResponse.json({
          questId: 1,
          teamId: 42,
          teamName: "Котики",
          status: "PENDING",
        } satisfies QuestRegistration);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<RegistrationPanel quest={BASE_QUEST} registrations={[]} />);

    expect(await screen.findByText(/ещё не подала заявку/i)).toBeInTheDocument();
    const button = screen.getByRole("button", { name: "Подать заявку" });
    await user.click(button);

    // Мутация должна пройти без ошибки — если бы POST ушёл на неверный URL
    // или с неверным телом, MSW (onUnhandledRequest: "error") или сам
    // компонент показали бы ошибку.
    await waitFor(() => expect(button).not.toBeDisabled());
    expect(screen.queryByText(/не удалось выполнить действие/i)).not.toBeInTheDocument();
  });

  it("квест в DRAFT, без заявки — регистрация ещё не открыта, кнопки нет", async () => {
    mockMyTeam(MY_TEAM);
    renderWithProviders(
      <RegistrationPanel quest={{ ...BASE_QUEST, status: "DRAFT" }} registrations={[]} />,
    );

    expect(await screen.findByText("Регистрация ещё не открыта.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Подать заявку" })).not.toBeInTheDocument();
  });

  it("квест в RUNNING, без заявки — регистрация закрыта", async () => {
    mockMyTeam(MY_TEAM);
    renderWithProviders(
      <RegistrationPanel quest={{ ...BASE_QUEST, status: "RUNNING" }} registrations={[]} />,
    );

    expect(await screen.findByText("Регистрация на этот квест закрыта.")).toBeInTheDocument();
  });

  it("заявка PENDING — показывает кнопку отмены, отмена уходит DELETE-запросом без ошибки", async () => {
    mockMyTeam(MY_TEAM);
    server.use(
      http.delete("/api/quests/register/:questId", () => {
        return HttpResponse.json({
          questId: 1,
          teamId: 42,
          teamName: "Котики",
          status: "PENDING",
        } satisfies QuestRegistration);
      }),
    );
    const pendingRegistration: QuestRegistration = {
      questId: 1,
      teamId: 42,
      teamName: "Котики",
      status: "PENDING",
    };
    const user = userEvent.setup();
    renderWithProviders(
      <RegistrationPanel quest={BASE_QUEST} registrations={[pendingRegistration]} />,
    );

    expect(await screen.findByText(/ожидает подтверждения/i)).toBeInTheDocument();
    const cancelButton = screen.getByRole("button", { name: "Отменить заявку" });
    await user.click(cancelButton);

    await waitFor(() => expect(cancelButton).not.toBeDisabled());
    expect(screen.queryByText(/не удалось выполнить действие/i)).not.toBeInTheDocument();
  });

  it("заявка APPROVED и квест RUNNING — показывает ссылку «Войти в игру» с правильным href", async () => {
    mockMyTeam(MY_TEAM);
    const approvedRegistration: QuestRegistration = {
      questId: 1,
      teamId: 42,
      teamName: "Котики",
      status: "APPROVED",
    };
    renderWithProviders(
      <RegistrationPanel
        quest={{ ...BASE_QUEST, status: "RUNNING" }}
        registrations={[approvedRegistration]}
      />,
    );

    expect(await screen.findByText("Ваша команда участвует в этом квесте.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Войти в игру" })).toHaveAttribute(
      "href",
      "/quests/1/play",
    );
  });

  it("заявка APPROVED, но квест ещё не RUNNING — не показывает ссылку на игру", async () => {
    mockMyTeam(MY_TEAM);
    const approvedRegistration: QuestRegistration = {
      questId: 1,
      teamId: 42,
      teamName: "Котики",
      status: "APPROVED",
    };
    renderWithProviders(
      <RegistrationPanel quest={BASE_QUEST} registrations={[approvedRegistration]} />,
    );

    expect(await screen.findByText(/игра начнётся/i)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Войти в игру" })).not.toBeInTheDocument();
  });

  it("заявка REJECTED — показывает сообщение об отклонении", async () => {
    mockMyTeam(MY_TEAM);
    const rejectedRegistration: QuestRegistration = {
      questId: 1,
      teamId: 42,
      teamName: "Котики",
      status: "REJECTED",
    };
    renderWithProviders(
      <RegistrationPanel quest={BASE_QUEST} registrations={[rejectedRegistration]} />,
    );

    expect(await screen.findByText("Заявка отклонена автором квеста.")).toBeInTheDocument();
  });

  it("известная гонка (roadmap.md §3): 409 при подаче заявки показывается как сообщение backend, а не общая ошибка", async () => {
    mockMyTeam(MY_TEAM);
    server.use(
      http.post("/api/quests/register/:questId/:teamId", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Conflict",
            status: 409,
            detail: "Лимит команд на квест уже достигнут.",
          },
          { status: 409 },
        );
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<RegistrationPanel quest={BASE_QUEST} registrations={[]} />);

    const button = await screen.findByRole("button", { name: "Подать заявку" });
    await user.click(button);

    expect(await screen.findByText("Лимит команд на квест уже достигнут.")).toBeInTheDocument();
  });
});
