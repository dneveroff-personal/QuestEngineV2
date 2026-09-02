import { useQuery } from "@tanstack/react-query";

import { getQuestById, getQuestRegistrations, getUpcomingQuests } from "@/api/quests";

export function useUpcomingQuests() {
  return useQuery({
    queryKey: ["quests", "upcoming"],
    queryFn: getUpcomingQuests,
  });
}

export function useQuest(questId: number) {
  return useQuery({
    queryKey: ["quests", questId],
    queryFn: () => getQuestById(questId),
  });
}

export function useQuestRegistrations(questId: number) {
  return useQuery({
    queryKey: ["quests", questId, "registrations"],
    queryFn: () => getQuestRegistrations(questId),
  });
}
