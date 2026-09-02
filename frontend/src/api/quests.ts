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

export function getUpcomingQuests(): Promise<QuestShort[]> {
  return apiFetch<QuestShort[]>("/api/quests/upcoming");
}

export function getQuestById(questId: number): Promise<Quest> {
  return apiFetch<Quest>(`/api/quests/${questId}`);
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
