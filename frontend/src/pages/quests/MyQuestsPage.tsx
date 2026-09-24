import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { getMyTeamQuests, type TeamQuestItem } from "@/api/teams";
import { useMyTeam } from "@/features/teams";
import { formatDateTime } from "@/lib/format";

const QUEST_STATUS_LABEL: Record<string, string> = {
  DRAFT: "Черновик",
  REGISTRATION: "Регистрация",
  RUNNING: "Идёт",
  FINISHED: "Завершён",
};

const REG_STATUS_LABEL: Record<string, string> = {
  PENDING: "Заявка на рассмотрении",
  APPROVED: "Заявка одобрена",
  REJECTED: "Заявка отклонена",
};

function TeamQuestCard({ item }: { item: TeamQuestItem }) {
  const canPlay = item.questStatus === "RUNNING" && item.registrationStatus === "APPROVED";

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <Link
        to={`/quests/${item.questId}`}
        className="block transition-colors hover:opacity-80"
      >
        <h3 className="font-medium">{item.title}</h3>
        <p className="text-muted-foreground text-sm">
          {QUEST_STATUS_LABEL[item.questStatus] ?? item.questStatus}
          {" · "}
          {REG_STATUS_LABEL[item.registrationStatus] ?? item.registrationStatus}
        </p>
        <p className="text-muted-foreground text-sm">
          Старт: {formatDateTime(item.startTime)}
        </p>
      </Link>
      <div className="flex flex-wrap gap-3 text-sm">
        {(item.questStatus === "RUNNING" || item.questStatus === "FINISHED") && (
          <Link
            to={`/quests/${item.questId}/statistics`}
            className="text-primary underline underline-offset-4"
          >
            Рейтинг
          </Link>
        )}
        {canPlay && (
          <Link
            to={`/quests/${item.questId}/play`}
            className="text-primary underline underline-offset-4"
          >
            Играть
          </Link>
        )}
      </div>
    </div>
  );
}

/** История регистраций команды: GET /api/teams/my/quests (включая FINISHED). */
export function MyQuestsPage() {
  const { data: myTeam, isLoading: isTeamLoading } = useMyTeam();

  const questsQuery = useQuery({
    queryKey: ["teams", "my", "quests"],
    queryFn: getMyTeamQuests,
    enabled: !!myTeam,
  });

  if (isTeamLoading) {
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

  if (questsQuery.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка...</p>;
  }

  if (questsQuery.isError) {
    return (
      <p className="text-destructive text-sm">
        Не удалось загрузить квесты. Попробуйте обновить страницу.
      </p>
    );
  }

  const items = questsQuery.data ?? [];
  const sorted = [...items].sort((a, b) => {
    const statusOrder = (s: string) =>
      s === "RUNNING" ? 0 : s === "REGISTRATION" ? 1 : s === "FINISHED" ? 2 : 3;
    const d = statusOrder(a.questStatus) - statusOrder(b.questStatus);
    if (d !== 0) return d;
    return Date.parse(b.startTime ?? "") - Date.parse(a.startTime ?? "");
  });

  const active = sorted.filter((i) => i.questStatus !== "FINISHED");
  const history = sorted.filter((i) => i.questStatus === "FINISHED");

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">Мои квесты</h1>
        <p className="text-muted-foreground text-sm">
          Регистрации вашей команды: текущие и история завершённых.
        </p>
      </div>

      {items.length === 0 && (
        <p className="text-muted-foreground text-sm">
          Команда ещё не подавала заявок на квесты.
        </p>
      )}

      {active.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-sm font-medium">Текущие</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {active.map((item) => (
              <TeamQuestCard key={item.registrationId} item={item} />
            ))}
          </div>
        </section>
      )}

      {history.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-sm font-medium">История</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {history.map((item) => (
              <TeamQuestCard key={item.registrationId} item={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
