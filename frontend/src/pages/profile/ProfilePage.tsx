/**
 * Профиль игрока (design-system.md §12): основная информация,
 * статистика, история игр, авторство.
 *
 * TODO: GET /users/me — уточнить точный путь в docs/04-api/endpoints.md.
 */
export function ProfilePage() {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Профиль</h1>
      <p className="text-muted-foreground text-sm">
        Личная информация и статистика появятся здесь.
      </p>
    </div>
  );
}
