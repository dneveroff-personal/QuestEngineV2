import { Navigate } from "react-router-dom";

import { useAuth } from "@/features/auth";
import { ManageAuthorsPanel } from "@/features/admin";

/** ADMIN-only: назначить / снять роль AUTHOR. */
export function ManageAuthorsPage() {
  const { role } = useAuth();

  if (role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Управление авторами</h1>
      <p className="text-muted-foreground text-sm">
        Назначение и снятие глобальной роли AUTHOR. Пользователь увидит пункт «Авторская» после
        повторного входа (роль в токене).
      </p>
      <ManageAuthorsPanel />
    </div>
  );
}
