import { apiFetch } from "@/api/client";

/** Сверено с HintResponse.java / CreateHintRequest.java (0.6.6: Size(max=2048) убран из валидации, но серверный лимит текста в БД не проверен — не полагаемся на клиентский max). */

export interface Hint {
  id: number;
  levelId: number;
  orderIndex: number;
  delaySeconds: number;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateHintRequest {
  content: string;
}

export function getHintsByLevel(questId: number, levelId: number): Promise<Hint[]> {
  return apiFetch<Hint[]>(`/api/quests/${questId}/levels/${levelId}/hints`);
}

export function createHint(
  questId: number,
  levelId: number,
  request: CreateHintRequest,
): Promise<Hint> {
  return apiFetch<Hint>(`/api/quests/${questId}/levels/${levelId}/hints`, {
    method: "POST",
    body: request,
  });
}

export function updateHint(hintId: number, request: CreateHintRequest): Promise<Hint> {
  return apiFetch<Hint>(`/api/hints/${hintId}`, { method: "PUT", body: request });
}

export function deleteHint(hintId: number): Promise<void> {
  return apiFetch<void>(`/api/hints/${hintId}`, { method: "DELETE" });
}
