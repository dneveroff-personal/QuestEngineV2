import { useMutation, useQueryClient } from "@tanstack/react-query";

import { transferCaptain, type Team } from "@/api/teams";
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
 *
 * ОБНОВЛЕНО (backend 0.6.11): TeamMemberDto теперь отдаёт userId напрямую
 * — резолв через /api/users/search (как раньше) больше не нужен для
 * transferCaptain. Раньше это было реальным риском (неоднозначный поиск
 * мог привести к передаче капитанства не тому человеку) — теперь просто
 * используем член команды напрямую.
 */
export function TeamMembersList({ team }: { team: Team }) {
  const { username } = useAuth();
  const queryClient = useQueryClient();

  const isCaptain = isCaptainOf(team, username);

  const transferMutation = useMutation({
    mutationFn: transferCaptain,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teams", "my"] });
    },
  });

  const errorMessage =
    transferMutation.error instanceof ApiError ? transferMutation.error.message : null;

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
                  onClick={() => transferMutation.mutate(member.userId)}
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
