import { apiFetch } from "@/api/client";

/** Сверено с TeamResponse.java / TeamMemberDto.java / CreateTeamRequest.java / TeamJoinResponse.java */

export type TeamRole = "CAPTAIN" | "MEMBER";
export type JoinRequestType = "JOIN_REQUEST" | "CAPTAIN_INVITE";

export interface TeamMember {
  id: number;
  userId: number;
  name: string;
  role: TeamRole;
  joinedAt: string;
}

export interface Team {
  id: number;
  name: string;
  captainName: string;
  createdAt: string;
  members: TeamMember[];
}

export interface CreateTeamRequest {
  name: string;
}

/**
 * ВАЖНО: `userName` здесь — это ВСЕГДА имя приглашаемого/вступающего
 * пользователя (TeamServiceImpl.buildTeamJoinResponse:
 * `request.getUser().getPublicName()`), не имя капитана-инициатора.
 *
 * Для капитана, просматривающего заявки НА СВОЮ команду (type=JOIN_REQUEST)
 * — это осмысленно: показывает, кто хочет вступить.
 *
 * Для пользователя, просматривающего ПОЛУЧЕННЫЕ приглашения
 * (type=CAPTAIN_INVITE) — `userName` совпадёт с именем самого пользователя
 * (он и есть request.user), а название пригласившей команды в ответе
 * вообще отсутствует. Это пробел в DTO на backend, не баг frontend —
 * см. docs/roadmap/backlog.md. UI ниже честно показывает "команда неизвестна"
 * вместо того, чтобы притворяться, что показывает что-то осмысленное.
 */
export interface TeamJoinRequestItem {
  requestId: number;
  userName: string;
  type: JoinRequestType;
  createdAt: string;
}

/**
 * Бросает ApiError со status=404 (TeamNotFoundException на backend), если
 * у пользователя ещё нет команды — это ОЖИДАЕМОЕ состояние, а не ошибка.
 * См. features/teams/useMyTeam.ts, где 404 превращается в null.
 */
export function getMyTeam(): Promise<Team> {
  return apiFetch<Team>("/api/teams/my");
}

export function createTeam(request: CreateTeamRequest): Promise<Team> {
  return apiFetch<Team>("/api/teams", { method: "POST", body: request });
}

export function searchTeams(name: string): Promise<Team[]> {
  const params = new URLSearchParams({ name });
  return apiFetch<Team[]>(`/api/teams/search?${params.toString()}`);
}

/**
 * Без username — заявка от себя на вступление в teamId (JOIN_REQUEST).
 * С username — приглашение конкретного пользователя капитаном
 * (CAPTAIN_INVITE); backend сам проверяет, что вызывающий — капитан
 * teamId (TeamServiceImpl.validateCaptain), 403 если нет.
 */
export function sendJoinRequest(teamId: number, username?: string): Promise<boolean> {
  const query = username ? `?username=${encodeURIComponent(username)}` : "";
  return apiFetch<boolean>(`/api/teams/${teamId}/request${query}`, { method: "POST" });
}

export function getJoinRequests(): Promise<TeamJoinRequestItem[]> {
  return apiFetch<TeamJoinRequestItem[]>("/api/teams/requests");
}

export function approveJoinRequest(requestId: number): Promise<boolean> {
  return apiFetch<boolean>(`/api/teams/requests/${requestId}/approve`, { method: "POST" });
}

export function rejectJoinRequest(requestId: number): Promise<boolean> {
  return apiFetch<boolean>(`/api/teams/requests/${requestId}/reject`, { method: "POST" });
}

/** Backend запрещает капитану покидать команду (нужно сначала передать капитанство) — вернёт ApiError. */
export function leaveTeam(): Promise<boolean> {
  return apiFetch<boolean>("/api/teams/leave", { method: "DELETE" });
}

export function transferCaptain(userId: number): Promise<boolean> {
  return apiFetch<boolean>(`/api/teams/transfer-captain/${userId}`, { method: "POST" });
}
