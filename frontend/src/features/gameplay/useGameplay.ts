import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  enterQuest,
  getCurrentLevel,
  getQuestProgress,
  listAuthorAttempts,
  listTeamAttempts,
  submitCode,
} from "@/api/gameplay";
import { getShownHints, takeHint } from "@/api/hints";

export function useQuestProgress(questId: number, teamId: number) {
  return useQuery({
    queryKey: ["gameplay", questId, teamId, "progress"],
    queryFn: () => getQuestProgress(questId, teamId),
    // Опрос вместо push — на backend нет SSE/WS для прогресса (ADR-0014
    // касается только статистики, не игрового прогресса). 5с — компромисс
    // между отзывчивостью и нагрузкой. Останавливаем опрос на FINISHED/DNF.
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "FINISHED" || status === "DNF" ? false : 5000;
    },
  });
}

/**
 * Агрегированный текущий уровень: title/content/autoTransitionAt/hints/mainCodes.
 * 404 (нет ACTIVE level) обрабатывается вызывающим кодом.
 */
export function useCurrentLevel(questId: number, teamId: number, enabled: boolean) {
  return useQuery({
    queryKey: ["gameplay", questId, teamId, "current-level"],
    queryFn: () => getCurrentLevel(questId, teamId),
    enabled,
    refetchInterval: 5000,
    retry: (failureCount, error) => {
      // 404 = нет активного уровня — не ретраим
      if (error && typeof error === "object" && "status" in error && error.status === 404) {
        return false;
      }
      return failureCount < 2;
    },
  });
}

export function useEnterQuest(questId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => enterQuest(questId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["gameplay", questId] });
    },
  });
}

export function useSubmitCode(questId: number, teamId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (value: string) => submitCode(questId, teamId, value),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["gameplay", questId, teamId] });
    },
  });
}

/**
 * Видимые подсказки (REGULAR показанные + BONUS/PENALTY доступные/взятые).
 * Polling — auto-reveal REGULAR на backend (Job 3, ADR-0020).
 */
export function useShownHints(questId: number, teamId: number, enabled = true) {
  return useQuery({
    queryKey: ["gameplay", questId, teamId, "hints"],
    queryFn: () => getShownHints(questId, teamId),
    enabled,
    refetchInterval: 5000,
  });
}

export function useTakeHint(questId: number, teamId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (hintId: number) => takeHint(questId, teamId, hintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["gameplay", questId, teamId, "hints"] });
      queryClient.invalidateQueries({ queryKey: ["gameplay", questId, teamId, "current-level"] });
    },
  });
}

/** Own team attempts on the currently ACTIVE level (GET .../codes). */
export function useTeamAttempts(questId: number, teamId: number, enabled = true) {
  return useQuery({
    queryKey: ["gameplay", questId, teamId, "attempts"],
    queryFn: () => listTeamAttempts(questId, teamId),
    enabled,
    // Same cadence as progress: new attempts appear after submit (invalidate) or teammate submit.
    refetchInterval: 5000,
  });
}

/** Author/ADMIN full attempt audit for the quest. */
export function useAuthorAttempts(questId: number, enabled = true) {
  return useQuery({
    queryKey: ["quests", questId, "code-submissions"],
    queryFn: () => listAuthorAttempts(questId),
    enabled,
  });
}
