import { apiFetch } from "@/api/client";

/** Сверено с LevelResponse.java / CreateLevelRequest.java */

export interface Level {
  id: number;
  questId: number;
  title: string;
  orderIndex: number;
  content: string;
  requiredMainCodesCount: number;
  timeoutSeconds: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLevelRequest {
  title: string;
  content?: string;
  timeoutSeconds?: number;
  requiredMainCodesCount?: number;
}

export function getLevelsByQuest(questId: number): Promise<Level[]> {
  return apiFetch<Level[]>(`/api/quests/${questId}/levels`);
}

export function createLevel(questId: number, request: CreateLevelRequest): Promise<Level> {
  return apiFetch<Level>(`/api/quests/${questId}/levels`, { method: "POST", body: request });
}

export function updateLevel(levelId: number, request: CreateLevelRequest): Promise<Level> {
  return apiFetch<Level>(`/api/levels/${levelId}`, { method: "PUT", body: request });
}

export function deleteLevel(levelId: number): Promise<void> {
  return apiFetch<void>(`/api/levels/${levelId}`, { method: "DELETE" });
}
