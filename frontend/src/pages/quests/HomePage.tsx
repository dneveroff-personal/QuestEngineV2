import { useUpcomingQuests, QuestCard } from "@/features/quests";

export function HomePage() {
  const { data: quests, isLoading, isError } = useUpcomingQuests();

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Квесты</h1>

      {isLoading && <p className="text-muted-foreground text-sm">Загрузка...</p>}

      {isError && (
        <p className="text-destructive text-sm">
          Не удалось загрузить список квестов. Попробуйте обновить страницу.
        </p>
      )}

      {quests && quests.length === 0 && (
        <p className="text-muted-foreground text-sm">Пока нет предстоящих квестов.</p>
      )}

      {quests && quests.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {quests.map((quest) => (
            <QuestCard key={quest.id} quest={quest} />
          ))}
        </div>
      )}
    </div>
  );
}
