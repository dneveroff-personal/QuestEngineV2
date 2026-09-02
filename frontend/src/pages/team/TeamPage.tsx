import { CreateTeamForm, useMyTeam } from "@/features/teams";
import { formatDateTime } from "@/lib/format";

const ROLE_LABEL: Record<string, string> = {
  CAPTAIN: "Капитан",
  MEMBER: "Участник",
};

/**
 * Полноценные формы вступления/поиска команды, передачи капитанства и
 * выхода из команды — следующий шаг (docs/frontend/roadmap.md, раздел 5,
 * пункт 3). Сейчас — экран для команды без команды (создание) и
 * read-only просмотр состава для тех, у кого команда уже есть.
 */
export function TeamPage() {
  const { data: team, isLoading, isError } = useMyTeam();

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка...</p>;
  }

  if (isError) {
    return (
      <p className="text-destructive text-sm">
        Не удалось загрузить данные о команде. Попробуйте обновить страницу.
      </p>
    );
  }

  if (!team) {
    return (
      <div className="max-w-sm space-y-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold">Команда</h1>
          <p className="text-muted-foreground text-sm">
            У вас пока нет команды. Создайте свою — или узнайте у капитана
            существующей команды её название, чтобы отправить заявку на
            вступление (поиск и вступление — в следующей версии).
          </p>
        </div>
        <CreateTeamForm />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">{team.name}</h1>
        <p className="text-muted-foreground text-sm">
          Капитан: {team.captainName} · Создана {formatDateTime(team.createdAt)}
        </p>
      </div>

      <div className="space-y-2">
        <h2 className="text-sm font-medium">Участники ({team.members.length})</h2>
        <ul className="divide-y divide-border rounded-lg border border-border">
          {team.members.map((member) => (
            <li key={member.id} className="flex items-center justify-between px-4 py-2 text-sm">
              <span>{member.name}</span>
              <span className="text-muted-foreground">{ROLE_LABEL[member.role] ?? member.role}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
