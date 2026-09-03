/**
 * Хранилище access-токена — в памяти (module-level переменная), не
 * localStorage/sessionStorage. Так токен недоступен через XSS-инъекцию,
 * читающую localStorage, и автоматически исчезает при закрытии вкладки.
 *
 * Плата за это: обновление страницы разлогинивает пользователя, пока
 * backend не реализует access+refresh (ADR-0015) — refresh token в
 * httpOnly cookie позволил бы тихо восстанавливать сессию при загрузке
 * страницы без участия JS. Сейчас backend этого не умеет (см.
 * roadmap/backlog.md), так что эта возможность всё равно недоступна вне
 * зависимости от того, где хранить access token. Когда backend
 * реализует refresh — здесь появится вызов POST /api/auth/refresh при
 * старте приложения (silent refresh), token.ts не изменится концептуально.
 *
 * Обычный module-singleton, а не React Context — потому что
 * api/client.ts должен синхронно читать токен вне React-дерева
 * (например, до монтирования компонентов).
 */

import { decodeJwtPayload } from "@/lib/jwt";

export interface AuthSession {
  token: string;
  publicName: string;
  /**
   * Из JWT (`sub`), не из LoginResponse — LoginResponse отдаёт только
   * publicName, а вся Team-модель на backend оперирует username
   * (TeamResponse.captainName, TeamMemberDto.name — оба
   * `User.getUsername()`, не `getPublicName()`). Без этого поля
   * невозможно было бы понять "это я?" в списке участников команды —
   * см. features/teams/TeamMembersList.tsx.
   */
  username: string | null;
  /** Из JWT (`role`) — только для UI (см. lib/jwt.ts). */
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

export function setSession(token: string, publicName: string): void {
  const payload = decodeJwtPayload(token);
  session = {
    token,
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

/** Для useSyncExternalStore — см. features/auth/useAuth.ts. */
export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
