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

/** ADR-0012 PageResponse from backend listing endpoints. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** GET /api/users/me — полный профиль текущего пользователя. */
export function getMe(): Promise<User> {
  return apiFetch<User>("/api/users/me");
}

/**
 * GET /api/users/search — для не-ADMIN email/role/createdAt могут быть null.
 * ADMIN получает полные поля. Ответ — PageResponse (ADR-0012).
 */
export function searchUsers(params: {
  username?: string;
  role?: UserRole;
  page?: number;
  size?: number;
}): Promise<PageResponse<User>> {
  const q = new URLSearchParams();
  if (params.username) q.set("username", params.username);
  if (params.role) q.set("role", params.role);
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  const qs = q.toString();
  return apiFetch<PageResponse<User>>(`/api/users/search${qs ? `?${qs}` : ""}`);
}

/** @deprecated Prefer searchUsers — backend returns PageResponse, not a bare array. */
export async function searchUsersByUsername(username: string): Promise<User[]> {
  const page = await searchUsers({ username, size: 50 });
  return page.content;
}

/** ADMIN-only: PUT /api/users/{userId}/role */
export function setUserRole(userId: number, role: UserRole): Promise<User> {
  return apiFetch<User>(`/api/users/${userId}/role`, {
    method: "PUT",
    body: { role },
  });
}
