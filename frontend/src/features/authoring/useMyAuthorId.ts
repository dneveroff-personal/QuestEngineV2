import { useQuery } from "@tanstack/react-query";

import { searchUsersByUsername } from "@/api/users";
import { useAuth } from "@/features/auth";

/**
 * QuestResponse НЕ отдаёт authorId ни в одном эндпоинте (ни getQuestById,
 * ни getUpcomingQuests) — GET /api/quests/authors/{authorId} существует,
 * но нужен authorId, а backend нигде не говорит frontend, какой у него
 * свой userId (нет GET /api/users/me — тот же пробел, что и в Team, см.
 * docs/roadmap/backlog.md).
 *
 * Резолвим через тот же /api/users/search, что и для transferCaptain, но
 * риск здесь ниже: худший случай ошибки — пустой/неверный список "моих
 * квестов" (сразу заметно, ничего необратимого), а не тихая передача
 * прав не тому человеку. Тем не менее при неоднозначном результате
 * честно отказываемся гадать — тот же принцип, что и в Team.
 */
export function useMyAuthorId() {
  const { username } = useAuth();

  return useQuery({
    queryKey: ["users", "resolve-my-id", username],
    queryFn: async () => {
      if (!username) return null;
      const candidates = await searchUsersByUsername(username);
      return candidates.length === 1 ? candidates[0].id : null;
    },
    enabled: !!username,
  });
}
