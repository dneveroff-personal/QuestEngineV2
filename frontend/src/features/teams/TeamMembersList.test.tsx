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

const team: Team = {
  id: 1,
  name: "Котики",
  captainUsername: "captain_user",
  captainDisplayName: "Captain User",
  createdAt: "2026-01-01T00:00:00Z",
  members: [
    {
      id: 10,
      userId: 100,
      username: "captain_user",
      displayName: "Captain User",
      role: "CAPTAIN",
      joinedAt: "2026-01-01T00:00:00Z",
    },
    {
      id: 11,
      userId: 101,
      username: "member_one",
      displayName: "Member One",
      role: "MEMBER",
      joinedAt: "2026-01-02T00:00:00Z",
    },
    {
      id: 12,
      userId: 102,
      username: "member_two",
      displayName: "Member Two",
      role: "MEMBER",
      joinedAt: "2026-01-03T00:00:00Z",
    },
  ],
};

afterEach(() => {
  clearSession();
});

describe("TeamMembersList", () => {
  it("hides transfer button for captain row when current user is captain", () => {
    loginAs("captain_user");
    renderWithProviders(<TeamMembersList team={team} />);

    const captainRow = screen.getByText("Captain User").closest("li")!;
    expect(
      within(captainRow).queryByRole("button", { name: "Сделать капитаном" }),
    ).not.toBeInTheDocument();

    const buttons = screen.getAllByRole("button", { name: "Сделать капитаном" });
    expect(buttons).toHaveLength(2);
  });

  it("hides all transfer buttons when current user is not captain", () => {
    loginAs("member_one");
    renderWithProviders(<TeamMembersList team={team} />);

    expect(screen.queryByRole("button", { name: "Сделать капитаном" })).not.toBeInTheDocument();
  });

  it("calls transferCaptain with member userId", async () => {
    const user = userEvent.setup();
    loginAs("captain_user");

    let transferredTo: string | null = null;
    server.use(
      http.post("/api/teams/transfer-captain/:userId", ({ params }) => {
        transferredTo = String(params.userId);
        return new HttpResponse(null, { status: 200 });
      }),
    );

    renderWithProviders(<TeamMembersList team={team} />);

    const memberOneRow = screen.getByText("Member One").closest("li")!;
    await user.click(within(memberOneRow).getByRole("button", { name: "Сделать капитаном" }));

    await waitFor(() => expect(transferredTo).toBe("101"));
  });
});
