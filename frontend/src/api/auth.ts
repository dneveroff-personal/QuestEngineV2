import { apiFetch } from "@/api/client";

/**
 * LoginResponse aligns with backend ADR-0015:
 * publicName, accessToken, refreshToken.
 */

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  publicName: string;
  accessToken: string;
  refreshToken: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  publicName?: string;
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/api/auth/login", {
    method: "POST",
    body: request,
  });
}

export function register(request: RegisterRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/api/auth/register", {
    method: "POST",
    body: request,
  });
}

export function refresh(refreshToken: string): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/api/auth/refresh", {
    method: "POST",
    body: { refreshToken },
  });
}

export function logout(refreshToken: string): Promise<void> {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
    body: { refreshToken },
  });
}
