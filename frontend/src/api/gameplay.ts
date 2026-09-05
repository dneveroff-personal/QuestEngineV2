import { apiFetch } from "@/api/client";

/** Сверено с QuestProgressResponse.java / CodeSubmissionResponse.java / SubmitCodeRequest.java / QuestProgressStatus.java / CodeSubmissionResult.java */

export type QuestProgressStatus = "WAITING" | "RUNNING" | "FINISHED" | "DNF";
export type CodeSubmissionResult = "CORRECT_MAIN" | "CORRECT_BONUS" | "CORRECT_PENALTY" | "INCORRECT";

export interface QuestProgress {
  teamName: string;
  status: QuestProgressStatus;
  questStartedAt: string | null;
  endedAt: string | null;
  finishedAt: string | null;
}

export interface CodeSubmissionResponse {
  result: CodeSubmissionResult;
  /** Осталось решить кодов до завершения уровня, null если на уровне нет обязательных кодов. */
  remainingMainCodes: number | null;
  levelCompleted: boolean;
  questFinished: boolean;
  submittedAt: string;
}

/** WAITING → RUNNING. QuestProgress должен уже существовать (создаётся, когда квест переходит в RUNNING) — если его ещё нет, backend вернёт ошибку. */
export function enterQuest(questId: number): Promise<QuestProgress> {
  return apiFetch<QuestProgress>(`/api/quests/progress/${questId}/enter`, { method: "POST" });
}

export function getQuestProgress(questId: number, teamId: number): Promise<QuestProgress> {
  return apiFetch<QuestProgress>(`/api/quests/progress/${questId}/${teamId}`);
}

/**
 * Не rate-limited намеренно (ADR-0016 — скорость ввода часть геймплея).
 * Ни один эндпоинт не говорит, на каком именно уровне сейчас команда, ни
 * его название/содержимое/таймер автоперехода (LevelProgressResponse
 * существует на backend, но не отдаётся ни одним контроллером — см.
 * docs/roadmap/backlog.md). Единственная обратная связь — эта самая
 * функция: remainingMainCodes/levelCompleted после каждой попытки.
 */
export function submitCode(questId: number, teamId: number, value: string): Promise<CodeSubmissionResponse> {
  return apiFetch<CodeSubmissionResponse>(`/api/quests/progress/${questId}/${teamId}/codes`, {
    method: "POST",
    body: { value },
  });
}
