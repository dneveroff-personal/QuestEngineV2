import { apiFetch } from "@/api/client";

/** Сверено с CodeResponse.java / CreateCodeRequest.java / CodeType.java */

export type CodeType = "MAIN" | "BONUS" | "PENALTY";

export interface Code {
  id: number;
  levelId: number;
  value: string;
  type: CodeType;
  points: number;
  codeIndex: number;
  createdAt: string;
}

export interface CreateCodeRequest {
  value: string;
  type: CodeType;
  codeIndex?: number;
  points?: number;
}

export function getCodesByLevel(questId: number, levelId: number): Promise<Code[]> {
  return apiFetch<Code[]>(`/api/quests/${questId}/levels/${levelId}/codes`);
}

export function createCode(
  questId: number,
  levelId: number,
  request: CreateCodeRequest,
): Promise<Code> {
  return apiFetch<Code>(`/api/quests/${questId}/levels/${levelId}/codes`, {
    method: "POST",
    body: request,
  });
}

export function updateCode(codeId: number, request: CreateCodeRequest): Promise<Code> {
  return apiFetch<Code>(`/api/codes/${codeId}`, { method: "PUT", body: request });
}

export function deleteCode(codeId: number): Promise<void> {
  return apiFetch<void>(`/api/codes/${codeId}`, { method: "DELETE" });
}
