import { useQuery } from "@tanstack/react-query";

import { searchUsersByUsername, type User } from "@/api/users";
import { useAuth } from "@/features/auth/useAuth";

/**
 * Общая логика для useMyAuthorId (authoring) и Profile — раньше была
 * продублирована в двух местах почти дословно. GET /api/users/me не
 * существует (docs/roadmap/backlog.md), поэтому email/createdAt/id
 * приходится резолвить через /api/users/search по своему же username —
 * при неоднозначном результате (0 или 2+ совпадений) честно возвращаем
 * null, а не гадаем (тот же принцип, что и в TeamMembersList.tsx для
 * transferCaptain, только там ставки выше).
 *
 * Живёт в features/auth, а не в authoring или profile — это общая
 * инфраструктура идентификации пользователя, а не специфика конкретной
 * feature (§7.1: другие features импортируют её через публичный API
 * auth, а не дублируют).
 */
export function useMyResolvedUser() {
  const { username } = useAuth();

  return useQuery({
    queryKey: ["users", "resolve-my-profile", username],
    queryFn: async (): Promise<User | null> => {
      if (!username) return null;
      const candidates = await searchUsersByUsername(username);
      return candidates.length === 1 ? candidates[0] : null;
    },
    enabled: !!username,
  });
}
