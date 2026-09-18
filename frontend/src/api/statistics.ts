import { apiFetch } from "@/api/client";
import type { QuestStatus } from "@/api/quests";

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

/** Ranking table snapshot. Available for RUNNING and FINISHED quests. */
export function getQuestStatistics(questId: number): Promise<QuestStatistics> {
  return apiFetch<QuestStatistics>(`/api/quests/${questId}/statistics`);
}
