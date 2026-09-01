/**
 * Главная страница — список доступных Quest.
 *
 * TODO: подключить GET /quests через api/quests.ts + useQuery, отрисовать
 * Quest Card (design-system.md §9). Backend-эндпоинт уже готов
 * (docs/04-api/endpoints.md) — можно реализовывать без моков.
 */
export function HomePage() {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Квесты</h1>
      <p className="text-muted-foreground text-sm">
        Здесь появится список квестов, доступных для участия.
      </p>
    </div>
  );
}
