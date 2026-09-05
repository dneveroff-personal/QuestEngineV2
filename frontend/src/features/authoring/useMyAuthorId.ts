import { useMyResolvedUser } from "@/features/auth";

/**
 * QuestResponse НЕ отдаёт authorId ни в одном эндпоинте (ни getQuestById,
 * ни getUpcomingQuests) — GET /api/quests/authors/{authorId} существует,
 * но нужен authorId, а backend нигде не говорит frontend, какой у него
 * свой userId (нет GET /api/users/me — тот же пробел, что и в Team, см.
 * docs/roadmap/backlog.md).
 *
 * Резолв вынесен в общий features/auth/useMyResolvedUser.ts (раньше был
 * продублирован здесь и в Profile) — риск здесь ниже, чем при
 * transferCaptain: худший случай ошибки — пустой/неверный список "моих
 * квестов" (сразу заметно, ничего необратимого), а не тихая передача
 * прав не тому человеку.
 */
export function useMyAuthorId() {
  const { data: user, isLoading, isError } = useMyResolvedUser();
  return { data: user?.id ?? null, isLoading, isError };
}
