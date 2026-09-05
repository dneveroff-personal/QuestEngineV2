import { useParams } from "react-router-dom";

import { ApiError } from "@/api/errors";
import { CodeSubmitForm, ShownHintsList, useEnterQuest, useQuestProgress } from "@/features/gameplay";
import { useMyTeam } from "@/features/teams";
import { Button } from "@/components/ui/button";
import { formatDateTime } from "@/lib/format";
import { NotFoundPage } from "@/pages/NotFoundPage";

const STATUS_LABEL: Record<string, string> = {
  WAITING: "Ожидание входа",
  RUNNING: "В процессе",
  FINISHED: "Завершено",
  DNF: "Не завершено (DNF)",
};

/**
 * ВАЖНО: backend не отдаёт ни через один эндпоинт название/содержимое
 * текущего уровня команды, ни таймер автоперехода (LevelProgressResponse
 * существует, но ни один контроллер её не возвращает — см.
 * docs/roadmap/backlog.md). Поэтому этот экран не показывает "легенду"
 * уровня — единственная обратная связь: результат ввода кода
 * (remainingMainCodes/levelCompleted) и список уже показанных подсказок.
 * Это честное ограничение текущего backend-контракта, не недосмотр
 * фронта — исправится само, когда появится нужный эндпоинт.
 */
export function GamePage() {
  const { questId: questIdParam } = useParams<{ questId: string }>();
  const questId = Number(questIdParam);
  const { data: myTeam, isLoading: isTeamLoading } = useMyTeam();

  if (!questIdParam || Number.isNaN(questId)) {
    return <NotFoundPage />;
  }

  if (isTeamLoading) {
    return <p className="text-muted-foreground p-4 text-sm">Загрузка...</p>;
  }

  if (!myTeam) {
    return <p className="text-destructive p-4 text-sm">У вас нет команды.</p>;
  }

  return <GamePageContent questId={questId} teamId={myTeam.id} teamName={myTeam.name} />;
}

function GamePageContent({
  questId,
  teamId,
  teamName,
}: {
  questId: number;
  teamId: number;
  teamName: string;
}) {
  const progressQuery = useQuestProgress(questId, teamId);
  const enterMutation = useEnterQuest(questId);

  if (progressQuery.isLoading) {
    return <p className="text-muted-foreground p-4 text-sm">Загрузка...</p>;
  }

  if (progressQuery.isError || !progressQuery.data) {
    const message =
      progressQuery.error instanceof ApiError
        ? progressQuery.error.message
        : "Не удалось загрузить прогресс. Возможно, квест ещё не начался.";
    return <p className="text-destructive p-4 text-sm">{message}</p>;
  }

  const progress = progressQuery.data;

  /**
   * Гонка (roadmap.md §3, Сценарий 2 в concurrency-scenarios.md):
   * POST .../enter не защищён от повторного вызова на backend. Наивный
   * `disabled={enterMutation.isPending}` оставляет окно между "мутация
   * завершилась" (isPending=false) и "рефетч progress подтянул новый
   * статус" — за это время повторный клик снова уйдёт в WAITING-ветку.
   * Решение: как только мутация СЕБЕ вернула успешный ответ, доверяем
   * его статусу немедленно, не дожидаясь инвалидации кэша.
   */
  const effectiveStatus = enterMutation.data?.status ?? progress.status;

  return (
    <div className="mx-auto max-w-2xl space-y-4 p-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">{teamName}</h1>
        <span className="text-muted-foreground text-sm">
          {STATUS_LABEL[effectiveStatus] ?? effectiveStatus}
        </span>
      </div>

      {effectiveStatus === "WAITING" && (
        <div className="rounded-lg border border-border p-4">
          <p className="text-sm">Квест начался. Нажмите, чтобы войти и начать первый уровень.</p>
          {enterMutation.error instanceof ApiError && (
            <p className="text-destructive text-sm">{enterMutation.error.message}</p>
          )}
          <Button
            className="mt-2"
            onClick={() => enterMutation.mutate()}
            disabled={enterMutation.isPending || enterMutation.isSuccess}
          >
            {enterMutation.isPending ? "Входим..." : "Войти в игру"}
          </Button>
        </div>
      )}

      {effectiveStatus === "RUNNING" && (
        <>
          <CodeSubmitForm questId={questId} teamId={teamId} />
          <ShownHintsList questId={questId} teamId={teamId} />
        </>
      )}

      {effectiveStatus === "FINISHED" && (
        <div className="rounded-lg border border-border p-4">
          <p className="text-success text-sm font-medium">Квест завершён!</p>
          {progress.finishedAt && (
            <p className="text-muted-foreground text-sm">
              Завершено: {formatDateTime(progress.finishedAt)}
            </p>
          )}
        </div>
      )}

      {effectiveStatus === "DNF" && (
        <div className="rounded-lg border border-border p-4">
          <p className="text-destructive text-sm font-medium">Квест не был завершён (DNF).</p>
        </div>
      )}
    </div>
  );
}
