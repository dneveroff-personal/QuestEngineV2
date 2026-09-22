import { apiFetch } from "@/api/client";

/** Сверено с TeamResponse.java / TeamMemberDto.java (username + displayName). */

export type TeamRole = "CAPTAIN" | "MEMBER";
export type JoinRequestType = "JOIN_REQUEST" | "CAPTAIN_INVITE";

export interface TeamMember {
  id: number;
  userId: number;
  /** Stable identity (login / JWT sub). */
  username: string;
  /** UI label: publicName if set, else username. */
  displayName: string;
  role: TeamRole;
  joinedAt: string;
}

export interface Team {
  id: number;
  name: string;
  captainUsername: string;
  captainDisplayName: string;
  captainName: string;
  createdAt: string;
  members: TeamMember[];
}

export interface CreateTeamRequest {
  name: string;
}

export interface TeamJoinRequestItem {
  requestId: number;
  userName: string;
  type: JoinRequestType;
  createdAt: string;
}

export type RegistrationStatus = "PENDING" | "APPROVED" | "REJECTED";
export type QuestStatus = "DRAFT" | "REGISTRATION" | "RUNNING" | "FINISHED";
export type QuestType = "SINGLE" | "TEAM";

/** Registration of the team on a quest + quest summary (backlog #9). */
export interface TeamQuestItem {
  registrationId: number;
  registrationStatus: RegistrationStatus;
  registrationCreatedAt: string;
  questId: number;
  title: string;
  description: string;
  type: QuestType;
  questStatus: QuestStatus;
  startTime: string | null;
  finishTime: string | null;
}

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

export function leaveTeam(): Promise<boolean> {
  return apiFetch<boolean>("/api/teams/leave", { method: "DELETE" });
}

export function transferCaptain(userId: number): Promise<boolean> {
  return apiFetch<boolean>(`/api/teams/transfer-captain/${userId}`, { method: "POST" });
}

export function getTeamQuests(teamId: number): Promise<TeamQuestItem[]> {
  return apiFetch<TeamQuestItem[]>(`/api/teams/${teamId}/quests`);
}

export function getMyTeamQuests(): Promise<TeamQuestItem[]> {
  return apiFetch<TeamQuestItem[]>("/api/teams/my/quests");
}
