import { useSyncExternalStore } from "react";

import { clearSession, getSession, setSession, subscribe } from "@/lib/auth-token";

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
    setSession,
    logout: clearSession,
  };
}
