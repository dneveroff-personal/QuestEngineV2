import { apiFetch } from "@/api/client";

/** Сверено с QuestProgressResponse / CodeSubmissionResponse / CurrentLevelResponse */

export type QuestProgressStatus = "WAITING" | "RUNNING" | "FINISHED" | "DNF";
export type CodeSubmissionResult =
  | "CORRECT_MAIN"
  | "CORRECT_BONUS"
  | "CORRECT_PENALTY"
  | "INCORRECT";
export type LevelProgressStatus = "ACTIVE" | "COMPLETED" | "AUTO_TRANSITIONED";
export type HintType = "REGULAR" | "BONUS" | "PENALTY";

export interface QuestProgress {
  id?: number;
  teamName: string;
  status: QuestProgressStatus;
  questStartedAt: string | null;
  endedAt: string | null;
  finishedAt: string | null;
  bonusPenaltySeconds?: number | null;
}

export interface CodeSubmissionResponse {
  result: CodeSubmissionResult;
  remainingMainCodes: number | null;
  levelCompleted: boolean;
  questFinished: boolean;
  submittedAt: string;
}

/** Visible hint during gameplay (same as GET .../hints). */
export interface HintProgressItem {
  hintId: number;
  orderIndex: number;
  content?: string | null;
  type: HintType;
  bonusPenaltySeconds?: number | null;
  shownAt?: string | null;
}

/**
 * Aggregated Game Mode view: ACTIVE LevelProgress + level content +
 * autoTransitionAt + hints + main-code progress.
 */
export interface CurrentLevel {
  levelProgressId: number;
  levelProgressStatus: LevelProgressStatus;
  openedAt: string | null;
  autoTransitionAt: string | null;
  levelId: number;
  orderIndex: number;
  title: string;
  content: string;
  requiredMainCodesCount: number | null;
  timeoutSeconds: number | null;
  mainCodesSolved: number;
  hints: HintProgressItem[];
}

export function enterQuest(questId: number): Promise<QuestProgress> {
  return apiFetch<QuestProgress>(`/api/quests/progress/${questId}/enter`, { method: "POST" });
}

export function getQuestProgress(questId: number, teamId: number): Promise<QuestProgress> {
  return apiFetch<QuestProgress>(`/api/quests/progress/${questId}/${teamId}`);
}

/**
 * Current ACTIVE level for the team. 404 if no active level.
 * Replaces N+1 over progress/hints/level for Game Mode.
 */
export function getCurrentLevel(questId: number, teamId: number): Promise<CurrentLevel> {
  return apiFetch<CurrentLevel>(`/api/quests/progress/${questId}/${teamId}/current-level`);
}

export function submitCode(
  questId: number,
  teamId: number,
  value: string,
): Promise<CodeSubmissionResponse> {
  return apiFetch<CodeSubmissionResponse>(`/api/quests/progress/${questId}/${teamId}/codes`, {
    method: "POST",
    body: { value },
  });
}

/** Сверено с CodeSubmissionAttemptResponse.java */
export type MatchedCodeType = "MAIN" | "BONUS" | "PENALTY";

export interface CodeSubmissionAttempt {
  id: number;
  rawValue: string;
  result: CodeSubmissionResult;
  submittedAt: string;
  submittedById: number;
  submittedByUsername: string;
  teamId: number;
  teamName: string;
  levelId: number;
  levelOrderIndex: number;
  levelProgressId: number;
  /** null when INCORRECT */
  matchedCodeId: number | null;
  matchedCodeIndex: number | null;
  matchedCodeType: MatchedCodeType | null;
}

/** Team: own attempts on currently ACTIVE LevelProgress (empty if none). */
export function listTeamAttempts(
  questId: number,
  teamId: number,
): Promise<CodeSubmissionAttempt[]> {
  return apiFetch<CodeSubmissionAttempt[]>(
    `/api/quests/progress/${questId}/${teamId}/codes`,
  );
}

/** Author/ADMIN: full attempt audit across all teams and levels. */
export function listAuthorAttempts(questId: number): Promise<CodeSubmissionAttempt[]> {
  return apiFetch<CodeSubmissionAttempt[]>(`/api/quests/${questId}/code-submissions`);
}
