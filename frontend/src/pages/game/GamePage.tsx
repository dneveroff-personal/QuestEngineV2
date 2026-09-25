import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { ApiError } from "@/api/errors";
import {
  CodeSubmitForm,
  ShownHintsList,
  TeamCodeAttemptsList,
  useCurrentLevel,
  useEnterQuest,
  useGameplaySocket,
  useQuestProgress,
} from "@/features/gameplay";
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

function formatCountdown(autoTransitionAt: string | null, nowMs: number): string | null {
  if (!autoTransitionAt) return null;
  const target = Date.parse(autoTransitionAt);
  if (Number.isNaN(target)) return null;
  const remainingSec = Math.max(0, Math.floor((target - nowMs) / 1000));
  const m = Math.floor(remainingSec / 60);
  const s = remainingSec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

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
  const progressQuery = useQuestProgress(questId, teamId, false);
  const enterMutation = useEnterQuest(questId);
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    const id = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

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
   * Гонка (Сценарий 2): после успешного enter доверяем статусу из ответа мутации
   * сразу, не дожидаясь рефетча progress.
   */
  const effectiveStatus = enterMutation.data?.status ?? progress.status;
  const isRunning = effectiveStatus === "RUNNING";

  return (
    <div className="mx-auto max-w-2xl space-y-4 p-4">
      <div className="flex items-center justify-between gap-2">
        <div className="space-y-0.5">
          <h1 className="text-xl font-semibold">{teamName}</h1>
          <span className="text-muted-foreground text-sm">
            {STATUS_LABEL[effectiveStatus] ?? effectiveStatus}
          </span>
        </div>
        <Link
          to={`/quests/${questId}/statistics`}
          className="text-primary shrink-0 text-sm underline underline-offset-4"
        >
          Рейтинг
        </Link>
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

      {isRunning && (
        <RunningGameplay
          questId={questId}
          teamId={teamId}
          questProgressId={progress.id}
          nowMs={nowMs}
        />
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

function RunningGameplay({
  questId,
  teamId,
  questProgressId,
  nowMs,
}: {
  questId: number;
  teamId: number;
  questProgressId: number;
  nowMs: number;
}) {
  const socketStatus = useGameplaySocket(questId, teamId, questProgressId, true);
  const live = socketStatus === "connected";

  // Re-bind progress with live flag so polling stops when WS is up
  useQuestProgress(questId, teamId, live);

  return (
    <>
      {socketStatus === "connecting" && (
        <p className="text-muted-foreground text-xs">Подключение realtime…</p>
      )}
      {socketStatus === "error" && (
        <p className="text-muted-foreground text-xs">
          Realtime недоступен — обновление по опросу (5 с).
        </p>
      )}
      <CurrentLevelPanel questId={questId} teamId={teamId} nowMs={nowMs} live={live} />
      <CodeSubmitForm questId={questId} teamId={teamId} />
      <TeamCodeAttemptsList questId={questId} teamId={teamId} live={live} />
      <ShownHintsList questId={questId} teamId={teamId} live={live} />
    </>
  );
}

function CurrentLevelPanel({
  questId,
  teamId,
  nowMs,
  live = false,
}: {
  questId: number;
  teamId: number;
  nowMs: number;
  live?: boolean;
}) {
  const levelQuery = useCurrentLevel(questId, teamId, true, live);

  if (levelQuery.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка уровня...</p>;
  }

  if (levelQuery.isError || !levelQuery.data) {
    const is404 =
      levelQuery.error instanceof ApiError && levelQuery.error.status === 404;
    if (is404) {
      return (
        <div className="rounded-lg border border-border p-4">
          <p className="text-muted-foreground text-sm">Нет активного уровня.</p>
        </div>
      );
    }
    const message =
      levelQuery.error instanceof ApiError
        ? levelQuery.error.message
        : "Не удалось загрузить текущий уровень.";
    return <p className="text-destructive text-sm">{message}</p>;
  }

  const level = levelQuery.data;
  const countdown = formatCountdown(level.autoTransitionAt, nowMs);
  const required =
    level.requiredMainCodesCount != null
      ? level.requiredMainCodesCount
      : null;

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-muted-foreground text-xs">
            Уровень {level.orderIndex}
          </p>
          <h2 className="text-base font-semibold">{level.title}</h2>
        </div>
        {countdown != null && (
          <div className="text-right">
            <p className="text-muted-foreground text-xs">Автопереход</p>
            <p className="font-mono text-sm tabular-nums">{countdown}</p>
          </div>
        )}
      </div>
      {level.content && (
        <div className="prose prose-sm dark:prose-invert max-w-none whitespace-pre-wrap text-sm">
          {level.content}
        </div>
      )}
      {required != null && (
        <p className="text-muted-foreground text-xs">
          Основных кодов: {level.mainCodesSolved} / {required}
        </p>
      )}
    </div>
  );
}
