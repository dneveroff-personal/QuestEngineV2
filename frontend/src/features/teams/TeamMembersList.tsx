import { useMutation, useQueryClient } from "@tanstack/react-query";

import { searchUsersByUsername } from "@/api/users";
import { transferCaptain, type Team, type TeamMember } from "@/api/teams";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth";
import { isCaptainOf } from "@/features/teams/utils";

const ROLE_LABEL: Record<string, string> = {
  CAPTAIN: "Капитан",
  MEMBER: "Участник",
};

/**
 * "Я капитан?" определяется сопоставлением своего username (из JWT, см.
 * lib/auth-token.ts) с team.members[].name — TeamMemberDto.name на
 * backend заполняется через User.getUsername(), НЕ getPublicName()
 * (TeamServiceImpl.teamMemberstoDto). publicName из LoginResponse для
 * этого сравнения не подходит — разные поля.
 */
export function TeamMembersList({ team }: { team: Team }) {
  const { username } = useAuth();
  const queryClient = useQueryClient();

  const isCaptain = isCaptainOf(team, username);

  const transferMutation = useMutation({
    /**
     * transferCaptain принимает userId, а TeamMemberDto.id — это id
     * записи TeamMember, НЕ User.id (TeamServiceImpl.teamMemberstoDto:
     * `m.getId()` — id самого TeamMember). Резолвим userId через
     * /api/users/search?username=... — но это LIKE-поиск, и UserResponse
     * не возвращает username обратно, так что при неоднозначном
     * совпадении НЕЛЬЗЯ молча брать первый результат (можно случайно
     * передать капитанство не тому человеку). См. docs/roadmap/backlog.md
     * — правильное решение: добавить username в UserResponse и/или
     * userId в TeamMemberDto на backend.
     */
    mutationFn: async (member: TeamMember) => {
      const candidates = await searchUsersByUsername(member.name);
      if (candidates.length !== 1) {
        throw new Error(
          candidates.length === 0
            ? `Не удалось найти пользователя "${member.name}".`
            : `Найдено несколько пользователей с похожим именем — не могу однозначно определить, кому передать капитанство. Обратитесь к администратору.`,
        );
      }
      return transferCaptain(candidates[0].id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teams", "my"] });
    },
  });

  const errorMessage =
    transferMutation.error instanceof ApiError
      ? transferMutation.error.message
      : transferMutation.error instanceof Error
        ? transferMutation.error.message
        : null;

  return (
    <div className="space-y-2">
      <h2 className="text-sm font-medium">Участники ({team.members.length})</h2>
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}
      <ul className="divide-y divide-border rounded-lg border border-border">
        {team.members.map((member) => (
          <li key={member.id} className="flex items-center justify-between px-4 py-2 text-sm">
            <span>{member.name}</span>
            <div className="flex items-center gap-3">
              <span className="text-muted-foreground">{ROLE_LABEL[member.role] ?? member.role}</span>
              {isCaptain && member.name !== username && (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => transferMutation.mutate(member)}
                  disabled={transferMutation.isPending}
                >
                  Сделать капитаном
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
