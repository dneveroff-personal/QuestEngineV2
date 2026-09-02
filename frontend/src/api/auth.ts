import { apiFetch } from "@/api/client";

/**
 * Соответствует backend DTO дословно (не придумано, сверено с исходниками):
 * - AuthRequestBase.java: username (3–64 симв.), password (мин. 5 симв.)
 * - LoginResponse.java: record LoginResponse(String publicName, String token)
 * - RegisterRequest.java: наследует AuthRequestBase + email, publicName
 *
 * Текущая модель — один JWT-токен без expiresIn/refreshToken в ответе
 * (единый JWT на 24ч, см. roadmap/backlog.md). Когда backend перейдёт на
 * access+refresh (ADR-0015), LoginResponse изменится — этот файл тоже
 * нужно будет обновить вместе с ним, это не отдельная работа "на потом".
 */

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  publicName: string;
  token: string;
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
