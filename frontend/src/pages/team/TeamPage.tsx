import {
  CreateTeamForm,
  JoinRequestsPanel,
  SearchTeamsForm,
  TeamManagementPanel,
  TeamMembersList,
  useMyTeam,
} from "@/features/teams";
import { formatDateTime } from "@/lib/format";

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
      <div className="max-w-sm space-y-8">
        <div className="space-y-4">
          <div className="space-y-1">
            <h1 className="text-2xl font-semibold">Команда</h1>
            <p className="text-muted-foreground text-sm">У вас пока нет команды.</p>
          </div>
          <CreateTeamForm />
        </div>

        <div className="space-y-2">
          <h2 className="text-sm font-medium">Или найдите существующую команду</h2>
          <SearchTeamsForm />
        </div>

        <JoinRequestsPanel />
      </div>
    );
  }

  return (
    <div className="max-w-lg space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">{team.name}</h1>
        <p className="text-muted-foreground text-sm">
          Капитан: {team.captainName} · Создана {formatDateTime(team.createdAt)}
        </p>
      </div>

      <TeamMembersList team={team} />
      <JoinRequestsPanel />
      <TeamManagementPanel team={team} />
    </div>
  );
}
