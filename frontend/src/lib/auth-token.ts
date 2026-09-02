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

export interface AuthSession {
  token: string;
  publicName: string;
}

let session: AuthSession | null = null;
const listeners = new Set<() => void>();

function notify() {
  for (const listener of listeners) listener();
}

export function getSession(): AuthSession | null {
  return session;
}

export function setSession(next: AuthSession): void {
  session = next;
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
