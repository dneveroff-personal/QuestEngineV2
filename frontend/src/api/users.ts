import { apiFetch } from "@/api/client";

/** Сверено с UserResponse.java. role — как в JWT (lib/jwt.ts), но здесь это факт от backend. */
export type UserRole = "PLAYER" | "AUTHOR" | "ADMIN";

export interface User {
  id: number;
  username?: string | null;
  publicName: string;
  email: string | null;
  role: UserRole | null;
  createdAt: string | null;
}

/** GET /api/users/me — полный профиль текущего пользователя. */
export function getMe(): Promise<User> {
  return apiFetch<User>("/api/users/me");
}

/** GET /api/users/search — для не-ADMIN email/role/createdAt могут быть null. */
export function searchUsersByUsername(username: string): Promise<User[]> {
  const params = new URLSearchParams({ username });
  return apiFetch<User[]>(`/api/users/search?${params.toString()}`);
}
