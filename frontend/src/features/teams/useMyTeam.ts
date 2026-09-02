import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/api/errors";
import { getMyTeam } from "@/api/teams";

/**
 * 404 от /api/teams/my означает "у пользователя ещё нет команды" — это
 * не ошибка запроса, а нормальный результат (TeamNotFoundException на
 * backend всегда бросается для пользователя без команды). Превращаем его
 * в `data: null`, а не в `isError`, чтобы компонентам не пришлось
 * различать "реальная ошибка сети" и "команды пока нет" вручную.
 */
export function useMyTeam() {
  return useQuery({
    queryKey: ["teams", "my"],
    queryFn: async () => {
      try {
        return await getMyTeam();
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          return null;
        }
        throw error;
      }
    },
  });
}
