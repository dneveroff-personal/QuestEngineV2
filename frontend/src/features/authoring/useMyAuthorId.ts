import { useMyResolvedUser } from "@/features/auth";

/** authorId из GET /api/users/me (useMyResolvedUser). */
export function useMyAuthorId() {
  const { data: user, isLoading, isError } = useMyResolvedUser();
  return { data: user?.id ?? null, isLoading, isError };
}
