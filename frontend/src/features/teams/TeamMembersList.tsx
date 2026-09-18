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
 * "Я капитан?" — сравнение JWT username с member.username.
 * На экране показывается member.displayName.
 * Transfer капитанства — по member.userId.
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
            <span>{member.displayName}</span>
            <div className="flex items-center gap-3">
              <span className="text-muted-foreground">{ROLE_LABEL[member.role] ?? member.role}</span>
              {isCaptain && member.username !== username && (
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
