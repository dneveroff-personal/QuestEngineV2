import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { registerTeamForQuest, unregisterTeam, type Quest, type QuestRegistration } from "@/api/quests";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { useMyTeam } from "@/features/teams";

interface RegistrationPanelProps {
  quest: Quest;
  registrations: QuestRegistration[];
}

/**
 * Вся логика статусов регистрации команды на квест — в одном месте, а не
 * размазана по QuestDetailPage (architecture.md §7, "Feature не должна
 * дублировать backend business logic" — здесь просто читаем состояние,
 * которое уже посчитал backend, а не пересчитываем сами).
 */
export function RegistrationPanel({ quest, registrations }: RegistrationPanelProps) {
  const { data: myTeam, isLoading: isTeamLoading } = useMyTeam();
  const queryClient = useQueryClient();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["quests", quest.id, "registrations"] });
  };

  const registerMutation = useMutation({
    mutationFn: (teamId: number) => registerTeamForQuest(quest.id, teamId),
    onSuccess: invalidate,
  });

  const unregisterMutation = useMutation({
    mutationFn: () => unregisterTeam(quest.id),
    onSuccess: invalidate,
  });

  if (isTeamLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка...</p>;
  }

  if (!myTeam) {
    return (
      <div className="rounded-lg border border-border p-4">
        <p className="text-muted-foreground text-sm">
          Чтобы участвовать, нужна команда.{" "}
          <Link to="/team" className="text-primary underline underline-offset-4">
            Создать или вступить в команду
          </Link>
        </p>
      </div>
    );
  }

  const myRegistration = registrations.find((r) => r.teamId === myTeam.id);

  const mutationError = registerMutation.error ?? unregisterMutation.error;
  const errorMessage =
    mutationError instanceof ApiError
      ? mutationError.message
      : mutationError
        ? "Не удалось выполнить действие. Попробуйте ещё раз."
        : null;

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}

      {!myRegistration && quest.status === "REGISTRATION" && (
        <>
          <p className="text-sm">Ваша команда «{myTeam.name}» ещё не подала заявку.</p>
          <Button
            onClick={() => registerMutation.mutate(myTeam.id)}
            disabled={registerMutation.isPending}
          >
            {registerMutation.isPending ? "Отправляем..." : "Подать заявку"}
          </Button>
        </>
      )}

      {!myRegistration && quest.status === "DRAFT" && (
        <p className="text-muted-foreground text-sm">Регистрация ещё не открыта.</p>
      )}

      {!myRegistration && (quest.status === "RUNNING" || quest.status === "FINISHED") && (
        <p className="text-muted-foreground text-sm">Регистрация на этот квест закрыта.</p>
      )}

      {myRegistration?.status === "PENDING" && (
        <>
          <p className="text-sm">Заявка отправлена, ожидает подтверждения автором.</p>
          <Button
            variant="outline"
            onClick={() => unregisterMutation.mutate()}
            disabled={unregisterMutation.isPending}
          >
            {unregisterMutation.isPending ? "Отменяем..." : "Отменить заявку"}
          </Button>
        </>
      )}

      {myRegistration?.status === "APPROVED" && (
        <p className="text-success text-sm font-medium">
          Ваша команда участвует в этом квесте.
          {/* TODO: ссылка на игровой режим — заблокировано backend
              (docs/frontend/roadmap.md, раздел 4.1: CodeSubmission/HintProgress runtime не реализованы). */}
        </p>
      )}

      {myRegistration?.status === "REJECTED" && (
        <p className="text-destructive text-sm">Заявка отклонена автором квеста.</p>
      )}
    </div>
  );
}
