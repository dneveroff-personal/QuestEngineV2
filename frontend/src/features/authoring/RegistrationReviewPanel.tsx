import { useMutation, useQueryClient } from "@tanstack/react-query";

import { approveTeamRegistration, rejectTeamRegistration } from "@/api/quests";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { useQuestRegistrations } from "@/features/quests";

/**
 * Автор-версия рассмотрения заявок — отдельно от features/quests/RegistrationPanel.tsx
 * (тот — со стороны команды: подать/отменить заявку). Здесь: капитан
 * квеста решает, кого допустить. Известная гонка на лимите команд
 * (roadmap.md §3, Сценарий 1) — 409 показываем как есть, не как
 * неизвестную ошибку.
 */
export function RegistrationReviewPanel({ questId }: { questId: number }) {
  const { data: registrations, isLoading } = useQuestRegistrations(questId);
  const queryClient = useQueryClient();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["quests", questId, "registrations"] });
  };

  const approveMutation = useMutation({
    mutationFn: (teamId: number) => approveTeamRegistration(questId, teamId),
    onSuccess: invalidate,
  });

  const rejectMutation = useMutation({
    mutationFn: (teamId: number) => rejectTeamRegistration(questId, teamId),
    onSuccess: invalidate,
  });

  const mutationError = approveMutation.error ?? rejectMutation.error;
  const errorMessage = mutationError instanceof ApiError ? mutationError.message : null;

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка заявок...</p>;
  }

  const pending = (registrations ?? []).filter((r) => r.status === "PENDING");
  const decided = (registrations ?? []).filter((r) => r.status !== "PENDING");

  return (
    <div className="space-y-3">
      <h2 className="text-sm font-medium">Заявки команд</h2>
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}

      {pending.length === 0 && decided.length === 0 && (
        <p className="text-muted-foreground text-sm">Заявок пока нет.</p>
      )}

      {pending.length > 0 && (
        <ul className="divide-y divide-border rounded-lg border border-border">
          {pending.map((registration) => (
            <li
              key={registration.teamId}
              className="flex items-center justify-between px-4 py-2 text-sm"
            >
              <span>{registration.teamName}</span>
              <div className="flex gap-2">
                <Button
                  size="sm"
                  onClick={() => approveMutation.mutate(registration.teamId)}
                  disabled={approveMutation.isPending || rejectMutation.isPending}
                >
                  Принять
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => rejectMutation.mutate(registration.teamId)}
                  disabled={approveMutation.isPending || rejectMutation.isPending}
                >
                  Отклонить
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {decided.length > 0 && (
        <ul className="text-muted-foreground divide-y divide-border rounded-lg border border-border text-sm">
          {decided.map((registration) => (
            <li key={registration.teamId} className="flex items-center justify-between px-4 py-2">
              <span>{registration.teamName}</span>
              <span>{registration.status === "APPROVED" ? "Принята" : "Отклонена"}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
