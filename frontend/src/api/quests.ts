import { apiFetch } from "@/api/client";

/** Сверено с QuestResponse.java / QuestShortProjection.java / QuestRegisterResponse.java */

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
  startTime: string;
  finishTime: string;
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
}

export function getUpcomingQuests(): Promise<QuestShort[]> {
  return apiFetch<QuestShort[]>("/api/quests/upcoming");
}

export function getQuestById(questId: number): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}`);
}

/**
 * Список квестов конкретного автора — не "мои квесты" сам по себе,
 * нужен authorId. QuestResponse НЕ отдаёт authorId нигде (ни здесь, ни в
 * getQuestById) — frontend вынужден резолвить свой userId окольным путём
 * (features/authoring/useMyAuthorId.ts). См. docs/roadmap/backlog.md.
 */
export function getQuestsByAuthor(authorId: number): Promise<Quest[]> {
  return apiFetch<Quest[]>(`/api/quests/authors/${authorId}`);
}

export function createQuest(request: CreateQuestRequest): Promise<Quest> {
  return apiFetch<Quest>("/api/quests", { method: "POST", body: request });
}

export function updateQuest(questId: number, request: CreateQuestRequest): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}`, { method: "PUT", body: request });
}

export function deleteQuest(questId: number): Promise<void> {
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

/** Отменяет регистрацию СВОЕЙ команды (backend берёт team из Authentication, не из параметра). */
export function unregisterTeam(questId: number): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(`/api/quests/register/${questId}`, {
    method: "DELETE",
  });
}

/** Только автор квеста (или ADMIN) — backend проверяет через validateQuestAuthor. */
export function approveTeamRegistration(questId: number, teamId: number): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(`/api/quests/register/${questId}/approve/${teamId}`, {
    method: "PUT",
  });
}

export function rejectTeamRegistration(questId: number, teamId: number): Promise<QuestRegistration> {
  return apiFetch<QuestRegistration>(`/api/quests/register/${questId}/teams/${teamId}/reject`, {
    method: "PUT",
  });
}
