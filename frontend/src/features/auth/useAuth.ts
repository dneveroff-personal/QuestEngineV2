import { useSyncExternalStore } from "react";

import { clearSession, getSession, subscribe } from "@/lib/auth-token";

/**
 * Реактивная обёртка над module-singleton из lib/auth-token.ts.
 * useSyncExternalStore — стандартный React-механизм для подписки на
 * состояние вне React-дерева, не требует ни Context Provider, ни
 * дополнительной библиотеки (Zustand и т.п. нигде не зафиксированы как
 * решение проекта — architecture.md не предполагает отдельного state
 * manager сверх TanStack Query для server state).
 */
export function useAuth() {
  const session = useSyncExternalStore(subscribe, getSession);

  return {
    isAuthenticated: session !== null,
    publicName: session?.publicName ?? null,
    /** Из JWT (`sub`) — см. lib/auth-token.ts. Нужен для сверки "это я?" в списках Team. */
    username: session?.username ?? null,
    /** Из JWT (`role`) — только для UI, не для авторизации (см. lib/jwt.ts). */
    role: session?.role ?? null,
    logout: clearSession,
  };
}
