import { useQueries, useQuery } from "@tanstack/react-query";

import { getQuestRegistrations, getUpcomingQuests } from "@/api/quests";
import { QuestCard } from "@/features/quests";
import { useMyTeam } from "@/features/teams";

/**
 * ВАЖНО: backend не даёт "квесты, на которые зарегистрирована моя
 * команда" напрямую — есть только GET /api/quests/register/{questId}
 * (по конкретному квесту), обратного эндпоинта нет (найдено при
 * реализации, см. docs/frontend/roadmap.md §4.3 и docs/roadmap/backlog.md).
 *
 * Временный обходной путь: берём upcoming-квесты и для каждого спрашиваем
 * его регистрации (N+1 запросов), фильтруем по своей команде на клиенте.
 * Это осознанный компромисс для небольшого pet-проекта (квестов немного),
 * НЕ паттерн для копирования в других местах. Ограничение: показывает
 * только "предстоящие" квесты — прошедшие (FINISHED), в которых команда
 * участвовала, сюда не попадут, т.к. /upcoming их не отдаёт.
 *
 * Удалить этот workaround, как только на backend появится
 * GET /api/teams/{teamId}/quests (или аналог).
 */
export function MyQuestsPage() {
  const { data: myTeam, isLoading: isTeamLoading } = useMyTeam();

  const questsQuery = useQuery({
    queryKey: ["quests", "upcoming"],
    queryFn: getUpcomingQuests,
  });

  const registrationQueries = useQueries({
    queries: (questsQuery.data ?? []).map((quest) => ({
      queryKey: ["quests", quest.id, "registrations"],
      queryFn: () => getQuestRegistrations(quest.id),
      enabled: !!myTeam,
    })),
  });

  if (isTeamLoading || questsQuery.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка...</p>;
  }

  if (!myTeam) {
    return (
      <p className="text-muted-foreground text-sm">
        У вас пока нет команды — вступите в команду или создайте свою на
        странице «Команда», чтобы участвовать в квестах.
      </p>
    );
  }

  if (questsQuery.isError) {
    return (
      <p className="text-destructive text-sm">
        Не удалось загрузить квесты. Попробуйте обновить страницу.
      </p>
    );
  }

  const isRegistrationsLoading = registrationQueries.some((q) => q.isLoading);
  const myQuests = (questsQuery.data ?? []).filter((_, index) =>
    registrationQueries[index]?.data?.some((r) => r.teamId === myTeam.id),
  );

  return (
    <div className="space-y-4">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">Мои квесты</h1>
        <p className="text-muted-foreground text-sm">
          Только предстоящие квесты, на которые подана заявка. История
          прошедших квестов появится вместе с соответствующим backend-эндпоинтом.
        </p>
      </div>

      {isRegistrationsLoading && <p className="text-muted-foreground text-sm">Загрузка...</p>}

      {!isRegistrationsLoading && myQuests.length === 0 && (
        <p className="text-muted-foreground text-sm">
          Ваша команда пока не подавала заявок на предстоящие квесты.
        </p>
      )}

      {myQuests.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {myQuests.map((quest) => (
            <QuestCard key={quest.id} quest={quest} />
          ))}
        </div>
      )}
    </div>
  );
}
