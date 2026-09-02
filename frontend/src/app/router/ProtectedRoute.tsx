import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { useAuth } from "@/features/auth";

/**
 * Защита маршрута (architecture.md §13: "redirect на authentication
 * screens; защиту UI-маршрутов" — это ответственность frontend). Не
 * заменяет проверку прав на backend (§14) — только UX: не показывать
 * экран, если и так придёт 401 на первый же запрос.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
