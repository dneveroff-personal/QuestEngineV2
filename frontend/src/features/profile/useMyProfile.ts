import { useQuery } from "@tanstack/react-query";

import { searchUsersByUsername, type User } from "@/api/users";
import { useAuth } from "@/features/auth";

/**
 * Тот же принцип, что и useMyAuthorId.ts: GET /api/users/me не
 * существует (см. docs/roadmap/backlog.md), поэтому email/createdAt
 * резолвятся через /api/users/search — при неоднозначном результате
 * честно возвращаем null, а не гадаем. publicName/username/role уже
 * доступны без этого запроса (см. useAuth — из LoginResponse и JWT).
 */
export function useMyProfile() {
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
