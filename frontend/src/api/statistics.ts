import { apiFetch } from "@/api/client";
import type { QuestStatus } from "@/api/quests";
import { getSession } from "@/lib/auth-token";

export type LevelProgressStatus = "ACTIVE" | "COMPLETED" | "AUTO_TRANSITIONED";
export type QuestProgressStatus = "WAITING" | "RUNNING" | "FINISHED" | "DNF";

export interface StatisticsLevelColumn {
  levelId: number;
  orderIndex: number;
  title: string;
}

export interface StatisticsLevelCell {
  orderIndex: number;
  levelId: number;
  status: LevelProgressStatus;
  openedAt: string | null;
  completedAt: string | null;
}

export interface StatisticsTeamRow {
  teamId: number;
  teamName: string;
  progressStatus: QuestProgressStatus;
  rank: number | null;
  levelsCompleted: number;
  lastCompletedLevelOrderIndex: number | null;
  lastLevelCompletedAt: string | null;
  levelCells: StatisticsLevelCell[];
  bonusPenaltySeconds: number;
  totalTimeSeconds: number | null;
}

export interface QuestStatistics {
  questId: number;
  questTitle: string;
  questStatus: QuestStatus;
  levels: StatisticsLevelColumn[];
  rows: StatisticsTeamRow[];
}

export function getQuestStatistics(questId: number): Promise<QuestStatistics> {
  return apiFetch<QuestStatistics>(`/api/quests/${questId}/statistics`);
}

/**
 * SSE live ranking (ADR-0014). Event name: `statistics` — full QuestStatistics snapshot.
 * Uses access_token query param because native EventSource cannot set Authorization.
 */
export function subscribeQuestStatistics(
  questId: number,
  onUpdate: (stats: QuestStatistics) => void,
  onError?: (err: Event) => void,
): () => void {
  const token = getSession()?.accessToken;
  const qs = token ? `?access_token=${encodeURIComponent(token)}` : "";
  const es = new EventSource(`/api/quests/${questId}/statistics/stream${qs}`);

  es.addEventListener("statistics", (ev) => {
    try {
      onUpdate(JSON.parse((ev as MessageEvent).data) as QuestStatistics);
    } catch {
      // ignore malformed
    }
  });

  es.onerror = (err) => {
    onError?.(err);
  };

  return () => es.close();
}
