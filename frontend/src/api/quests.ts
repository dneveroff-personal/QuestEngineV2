import { apiFetch } from "@/api/client";

/** Сверено с QuestResponse.java / CreateQuestRequest.java */

export type QuestType = "SINGLE" | "TEAM";
export type QuestStatus = "DRAFT" | "REGISTRATION" | "RUNNING" | "FINISHED";
export type RegistrationStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface QuestShort {
  id: number;
  title: string;
  startTime: string;
}

export interface Quest {
  id: number;
  title: string;
  description: string;
  type: QuestType;
  status: QuestStatus;
  createdAt: string;
  startTime: string | null;
  finishTime: string | null;
  maximumTeams?: number;
  archived?: boolean;
  authorId?: number;
  authorName?: string;
}

export interface QuestRegistration {
  questId: number;
  teamId: number;
  teamName: string;
  status: RegistrationStatus;
}

export interface CreateQuestRequest {
  title: string;
  description: string;
  type: QuestType;
  startTime?: string | null;
  finishTime?: string | null;
  maximumTeams?: number | null;
}

export function getUpcomingQuests(): Promise<QuestShort[]> {
  return apiFetch<QuestShort[]>("/api/quests/upcoming");
}

export function getQuestById(questId: number): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}`);
}

/** Список квестов автора. В QuestResponse есть authorId/authorName. */
export function getQuestsByAuthor(authorId: number): Promise<Quest[]> {
  return apiFetch<Quest[]>(`/api/quests/authors/${authorId}`);
}

export function createQuest(request: CreateQuestRequest): Promise<Quest> {
  return apiFetch<Quest>("/api/quests", { method: "POST", body: request });
}

export function updateQuest(questId: number, request: CreateQuestRequest): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}`, { method: "PUT", body: request });
}

/** Soft-delete: помечает квест архивным, история регистраций/прохождения не теряется. */
export function archiveQuest(questId: number): Promise<void> {
  return apiFetch<void>(`/api/quests/${questId}`, { method: "DELETE" });
}

/** DRAFT → REGISTRATION */
export function publishQuest(questId: number): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}/publish`, { method: "POST" });
}

/** RUNNING → FINISHED. Команды, не завершившие квест, получают DNF (backend). */
export function finishQuest(questId: number): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}/finish`, { method: "POST" });
}

/** Все регистрации по квесту (список команд), не только своей. */
export function getQuestRegistrations(questId: number): Promise<QuestRegistration[]> {
  return apiFetch<QuestRegistration[]>(`/api/quests/register/${questId}`);
}

export function registerTeamForQuest(questId: number, teamId: number): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(`/api/quests/register/${questId}/${teamId}`, {
    method: "POST",
  });
}

/** Отменяет регистрацию СВОЕЙ команды (backend берёт team из Authentication). */
export function unregisterTeam(questId: number): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(`/api/quests/register/${questId}`, {
    method: "DELETE",
  });
}

export function approveTeamRegistration(
  questId: number,
  teamId: number,
): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(`/api/quests/register/${questId}/approve/${teamId}`, {
    method: "PUT",
  });
}

export function rejectTeamRegistration(
  questId: number,
  teamId: number,
): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(
    `/api/quests/register/${questId}/teams/${teamId}/reject`,
    { method: "PUT" },
  );
}
