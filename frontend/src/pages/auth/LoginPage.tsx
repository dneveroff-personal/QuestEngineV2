/**
 * Экран входа.
 *
 * TODO (следующий шаг реализации): форма на react-hook-form + zod
 * (architecture.md §12.1), POST /auth/login через api/auth.ts,
 * сохранение access token, redirect на главную.
 *
 * Важно: текущий backend реализует старую auth-модель (единый JWT на
 * 24ч), а не access+refresh из ADR-0015 — интерцептор на 401→refresh
 * (architecture.md §9.1) не заработает, пока backend не доработан
 * (см. roadmap/backlog.md).
 */
export function LoginPage() {
  return (
    <div className="mx-auto max-w-sm space-y-4">
      <h1 className="text-2xl font-semibold">Вход</h1>
      <p className="text-muted-foreground text-sm">Форма входа появится здесь.</p>
    </div>
  );
}
