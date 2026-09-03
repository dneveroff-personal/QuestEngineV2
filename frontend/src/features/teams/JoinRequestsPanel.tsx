import { useMutation, useQueryClient } from "@tanstack/react-query";

import { approveJoinRequest, rejectJoinRequest } from "@/api/teams";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/api/errors";
import { formatDateTime } from "@/lib/format";
import { useJoinRequests } from "@/features/teams/useJoinRequests";

/**
 * Один и тот же список с двумя разными смыслами (backend решает это сам,
 * см. api/teams.ts комментарий над TeamJoinRequestItem):
 * - type=JOIN_REQUEST — заявки на вступление в вашу команду (вы капитан).
 * - type=CAPTAIN_INVITE — приглашения, полученные вами от других команд.
 *   Название пригласившей команды backend не отдаёт — честно показываем
 *   это ограничение, а не выдумываем.
 */
export function JoinRequestsPanel() {
  const { data: requests, isLoading } = useJoinRequests();
  const queryClient = useQueryClient();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["teams", "requests"] });
    queryClient.invalidateQueries({ queryKey: ["teams", "my"] });
  };

  const approveMutation = useMutation({
    mutationFn: approveJoinRequest,
    onSuccess: invalidate,
  });

  const rejectMutation = useMutation({
    mutationFn: rejectJoinRequest,
    onSuccess: invalidate,
  });

  const mutationError = approveMutation.error ?? rejectMutation.error;
  const errorMessage =
    mutationError instanceof ApiError
      ? mutationError.message
      : mutationError
        ? "Не удалось выполнить действие."
        : null;

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка заявок...</p>;
  }

  if (!requests || requests.length === 0) {
    return null;
  }

  return (
    <div className="space-y-2">
      <h2 className="text-sm font-medium">Заявки и приглашения</h2>
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}
      <ul className="divide-y divide-border rounded-lg border border-border">
        {requests.map((request) => (
          <li
            key={request.requestId}
            className="flex items-center justify-between gap-3 px-4 py-2 text-sm"
          >
            <div>
              {request.type === "JOIN_REQUEST" ? (
                <p>
                  <span className="font-medium">{request.userName}</span> хочет вступить в команду
                </p>
              ) : (
                <p>Приглашение в команду (название недоступно)</p>
              )}
              <p className="text-muted-foreground text-xs">{formatDateTime(request.createdAt)}</p>
            </div>
            <div className="flex gap-2">
              <Button
                size="sm"
                onClick={() => approveMutation.mutate(request.requestId)}
                disabled={approveMutation.isPending || rejectMutation.isPending}
              >
                {request.type === "JOIN_REQUEST" ? "Принять" : "Согласиться"}
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => rejectMutation.mutate(request.requestId)}
                disabled={approveMutation.isPending || rejectMutation.isPending}
              >
                Отклонить
              </Button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
