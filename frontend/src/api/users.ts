import { apiFetch } from "@/api/client";

/** Сверено с UserResponse.java. role — как в JWT (lib/jwt.ts), но здесь это факт от backend, не декодированный клиентом. */
export type UserRole = "PLAYER" | "AUTHOR" | "ADMIN";

export interface User {
  id: number;
  publicName: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

/**
 * GET /api/users/search не требует ADMIN (в отличие от setUserRole/
 * resetPassword, которые проверяют роль внутри UserServiceImpl) — доступен
 * любому аутентифицированному пользователю. Используется здесь только для
 * резолва username → userId (нужно для transferCaptain — см.
 * features/teams/TeamMembersList.tsx), не как админ-функция.
 */
export function searchUsersByUsername(username: string): Promise<User[]> {
  const params = new URLSearchParams({ username });
  return apiFetch<User[]>(`/api/users/search?${params.toString()}`);
}
