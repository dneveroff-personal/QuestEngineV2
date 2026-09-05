import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { enterQuest, getQuestProgress, submitCode } from "@/api/gameplay";
import { getShownHints } from "@/api/hints";

export function useQuestProgress(questId: number, teamId: number) {
  return useQuery({
    queryKey: ["gameplay", questId, teamId, "progress"],
    queryFn: () => getQuestProgress(questId, teamId),
    // Опрос вместо push — на backend нет SSE/WS для прогресса (ADR-0014
    // касается только статистики, не игрового прогресса). 5с — компромисс
    // между отзывчивостью и нагрузкой, не основано на каком-то бэкенд-SLA.
    refetchInterval: 5000,
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
 * Тоже опрос, не push — те же ограничения, что и у useQuestProgress.
 * Подсказки появляются по таймеру на backend (auto-reveal, ADR-0020) —
 * без polling команда узнает о новой подсказке только обновив страницу.
 */
export function useShownHints(questId: number, teamId: number) {
  return useQuery({
    queryKey: ["gameplay", questId, teamId, "hints"],
    queryFn: () => getShownHints(questId, teamId),
    refetchInterval: 5000,
  });
}
