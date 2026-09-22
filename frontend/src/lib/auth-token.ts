/**
 * Access token in memory; refresh token also in memory for SPA same-origin MVP.
 * On 401 the API client rotates via POST /api/auth/refresh (ADR-0015).
 */

import { decodeJwtPayload } from "@/lib/jwt";

export interface AuthSession {
  token: string;
  accessToken: string;
  refreshToken: string;
  publicName: string;
  username: string | null;
  role: string | null;
}

let session: AuthSession | null = null;
const listeners = new Set<() => void>();

function notify() {
  for (const listener of listeners) listener();
}

export function getSession(): AuthSession | null {
  return session;
}

export function setSession(
  accessToken: string,
  publicName: string,
  refreshToken: string,
): void {
  const payload = decodeJwtPayload(accessToken);
  session = {
    token: accessToken,
    accessToken,
    refreshToken,
    publicName,
    username: payload?.sub ?? null,
    role: payload?.role ?? null,
  };
  notify();
}

export function clearSession(): void {
  session = null;
  notify();
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
