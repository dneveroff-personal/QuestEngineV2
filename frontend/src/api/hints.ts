import { apiFetch } from "@/api/client";

/** Сверено с HintResponse.java / CreateHintRequest.java / HintProgressResponse.java / HintType.java */

export type HintType = "REGULAR" | "BONUS" | "PENALTY";

export interface Hint {
  id: number;
  levelId: number;
  orderIndex: number;
  delaySeconds: number;
  content: string;
  type: HintType;
  bonusPenaltySeconds: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateHintRequest {
  orderIndex: number;
  delaySeconds: number;
  content: string;
  type: HintType;
  bonusPenaltySeconds: number | null;
}

export interface ShownHint {
  hintId: number;
  orderIndex: number;
  content: string;
  type: HintType;
  bonusPenaltySeconds: number | null;
  shownAt: string;
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

/** Подсказки, уже показанные команде на её текущем активном уровне (auto-reveal, ADR-0020). */
export function getShownHints(questId: number, teamId: number): Promise<ShownHint[]> {
  return apiFetch<ShownHint[]>(`/api/quests/${questId}/${teamId}/hints`);
}
