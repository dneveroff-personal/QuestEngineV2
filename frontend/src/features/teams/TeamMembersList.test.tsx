import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import type { Team } from "@/api/teams";
import { TeamMembersList } from "@/features/teams/TeamMembersList";
import { clearSession } from "@/lib/auth-token";
import { loginAs } from "@/test/fixtures";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";

const TEAM: Team = {
  id: 1,
  name: "Котики",
  captainName: "captain_user",
  createdAt: "2026-01-01T00:00:00Z",
  members: [
    { id: 10, userId: 100, name: "captain_user", role: "CAPTAIN", joinedAt: "2026-01-01T00:00:00Z" },
    { id: 11, userId: 101, name: "member_one", role: "MEMBER", joinedAt: "2026-01-02T00:00:00Z" },
    { id: 12, userId: 102, name: "member_two", role: "MEMBER", joinedAt: "2026-01-03T00:00:00Z" },
  ],
};

afterEach(() => {
  clearSession();
});

describe("TeamMembersList", () => {
  it("капитан видит «Сделать капитаном» у остальных, но не у себя", async () => {
    loginAs("captain_user");
    renderWithProviders(<TeamMembersList team={TEAM} />);

    // У себя (captain_user) кнопки нет — ищем строку по имени и проверяем отсутствие кнопки внутри неё.
    const captainRow = screen.getByText("captain_user").closest("li")!;
    expect(
      within(captainRow).queryByRole("button", { name: "Сделать капитаном" }),
    ).not.toBeInTheDocument();

    // У остальных двоих — есть.
    const buttons = screen.getAllByRole("button", { name: "Сделать капитаном" });
    expect(buttons).toHaveLength(2);
  });

  it("обычный участник не видит кнопку «Сделать капитаном» ни у кого", () => {
    loginAs("member_one");
    renderWithProviders(<TeamMembersList team={TEAM} />);

    expect(screen.queryByRole("button", { name: "Сделать капитаном" })).not.toBeInTheDocument();
  });

  it("передача капитанства отправляет правильный userId и завершается без ошибки", async () => {
    loginAs("captain_user");
    let capturedUserId: string | undefined;
    server.use(
      http.post("/api/teams/transfer-captain/:userId", ({ params }) => {
        capturedUserId = params.userId as string;
        return HttpResponse.json(true);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<TeamMembersList team={TEAM} />);

    const memberOneRow = screen.getByText("member_one").closest("li")!;
    await user.click(within(memberOneRow).getByRole("button", { name: "Сделать капитаном" }));

    await waitFor(() => expect(capturedUserId).toBe("101"));
  });

  it("ошибка передачи капитанства показывается пользователю", async () => {
    loginAs("captain_user");
    server.use(
      http.post("/api/teams/transfer-captain/:userId", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Forbidden",
            status: 403,
            detail: "Только капитан может передать капитанство.",
          },
          { status: 403 },
        );
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<TeamMembersList team={TEAM} />);

    const memberRow = screen.getByText("member_one").closest("li")!;
    await user.click(within(memberRow).getByRole("button", { name: "Сделать капитаном" }));

    expect(await screen.findByText("Только капитан может передать капитанство.")).toBeInTheDocument();
  });
});
