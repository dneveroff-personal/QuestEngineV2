import { apiFetch } from "@/api/client";

/** Сверено с TeamResponse.java / TeamMemberDto.java / CreateTeamRequest.java */

export type TeamRole = "CAPTAIN" | "MEMBER";

export interface TeamMember {
  id: number;
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
