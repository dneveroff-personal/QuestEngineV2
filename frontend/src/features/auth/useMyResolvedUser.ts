import { useQuery } from "@tanstack/react-query";

import { getMe, type User } from "@/api/users";
import { useAuth } from "@/features/auth/useAuth";

/** Полный профиль текущего пользователя: GET /api/users/me. */
export function useMyResolvedUser() {
  const { username } = useAuth();

  return useQuery({
    queryKey: ["users", "me", username],
    queryFn: (): Promise<User> => getMe(),
    enabled: !!username,
  });
}
