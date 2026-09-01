/**
 * Страница команды (design-system.md §11).
 *
 * Основная информация: название, капитан, участники, статистика, история
 * игр. "Управление командой" видит только капитан — на этом же экране,
 * не в отдельном интерфейсе.
 *
 * TODO: GET /teams/{teamId}, роль текущего пользователя — из auth state.
 */
export function TeamPage() {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Команда</h1>
      <p className="text-muted-foreground text-sm">
        Информация о команде появится здесь.
      </p>
    </div>
  );
}
