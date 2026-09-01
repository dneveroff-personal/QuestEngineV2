/**
 * "Мои квесты" — квесты, в которых участвует текущая команда/игрок.
 *
 * TODO: GET /quests?participant=me (уточнить контракт в
 * docs/04-api/endpoints.md, если параметр ещё не зафиксирован).
 */
export function MyQuestsPage() {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Мои квесты</h1>
      <p className="text-muted-foreground text-sm">
        Квесты, в которых вы участвуете, появятся здесь.
      </p>
    </div>
  );
}
