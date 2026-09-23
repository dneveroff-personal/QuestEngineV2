import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { ApiError } from "@/api/errors";
import {
  getQuestStatistics,
  subscribeQuestStatistics,
  type QuestStatistics,
  type StatisticsTeamRow,
} from "@/api/statistics";
import { formatDateTime, formatDurationSeconds } from "@/lib/format";
import { NotFoundPage } from "@/pages/NotFoundPage";

const QUEST_STATUS_LABEL: Record<string, string> = {
  DRAFT: "Черновик",
  REGISTRATION: "Регистрация",
  RUNNING: "Идёт",
  FINISHED: "Завершён",
};

const PROGRESS_LABEL: Record<string, string> = {
  WAITING: "Ожидание",
  RUNNING: "В игре",
  FINISHED: "Финиш",
  DNF: "DNF",
};

/**
 * Таблица ranking из statistics-ranking.md:
 * - runtime: место по числу завершённых уровней / времени предыдущего;
 * - final (FINISHED): по totalTimeSeconds (+ DNF внизу);
 * - команды с 0 completed levels не в таблице (правило 3).
 *
 * Live: SSE при questStatus === RUNNING (ADR-0014).
 */
export function StatisticsPage() {
  const { questId: questIdParam } = useParams<{ questId: string }>();
  const questId = Number(questIdParam);

  const [stats, setStats] = useState<QuestStatistics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [live, setLive] = useState(false);

  useEffect(() => {
    if (!questIdParam || Number.isNaN(questId)) return;

    let cancelled = false;
    let unsubscribe: (() => void) | undefined;

    setLoading(true);
    setError(null);

    getQuestStatistics(questId)
      .then((data) => {
        if (cancelled) return;
        setStats(data);
        setLoading(false);

        if (data.questStatus === "RUNNING") {
          setLive(true);
          unsubscribe = subscribeQuestStatistics(
            questId,
            (next) => {
              if (!cancelled) setStats(next);
            },
            () => {
              // EventSource reconnects itself; keep last snapshot
            },
          );
        } else {
          setLive(false);
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setLoading(false);
        setError(
          err instanceof ApiError
            ? err.message
            : "Не удалось загрузить статистику.",
        );
      });

    return () => {
      cancelled = true;
      unsubscribe?.();
    };
  }, [questId, questIdParam]);

  if (!questIdParam || Number.isNaN(questId)) {
    return <NotFoundPage />;
  }

  if (loading) {
    return <p className="text-muted-foreground p-4 text-sm">Загрузка статистики...</p>;
  }

  if (error || !stats) {
    return (
      <div className="space-y-2 p-4">
        <p className="text-destructive text-sm">{error ?? "Нет данных."}</p>
        <Link to={`/quests/${questId}`} className="text-primary text-sm underline">
          К квесту
        </Link>
      </div>
    );
  }

  const levels = [...stats.levels].sort((a, b) => a.orderIndex - b.orderIndex);
  const rows = [...stats.rows].sort((a, b) => {
    if (a.rank != null && b.rank != null) return a.rank - b.rank;
    if (a.rank != null) return -1;
    if (b.rank != null) return 1;
    return a.teamName.localeCompare(b.teamName);
  });

  return (
    <div className="space-y-4 p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold">{stats.questTitle}</h1>
          <p className="text-muted-foreground text-sm">
            {QUEST_STATUS_LABEL[stats.questStatus] ?? stats.questStatus}
            {live ? " · live" : ""}
          </p>
        </div>
        <div className="flex gap-3 text-sm">
          <Link to={`/quests/${questId}`} className="text-primary underline underline-offset-4">
            Квест
          </Link>
          {(stats.questStatus === "RUNNING" || stats.questStatus === "FINISHED") && (
            <Link
              to={`/quests/${questId}/play`}
              className="text-primary underline underline-offset-4"
            >
              Игра
            </Link>
          )}
        </div>
      </div>

      {rows.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          Пока нет команд в таблице (команды появляются после завершения первого уровня).
        </p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-border">
          <table className="w-full min-w-[640px] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/40">
                <th className="px-3 py-2 font-medium">#</th>
                <th className="px-3 py-2 font-medium">Команда</th>
                {levels.map((col) => (
                  <th key={col.levelId} className="px-3 py-2 font-medium whitespace-nowrap">
                    L{col.orderIndex}
                  </th>
                ))}
                <th className="px-3 py-2 font-medium">±BP</th>
                <th className="px-3 py-2 font-medium">Итог</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <StatisticsRow key={row.teamId} row={row} levels={levels} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      <p className="text-muted-foreground text-xs">
        Ranking: больше пройденных уровней выше; при равенстве — кто раньше закончил предыдущий.
        После FINISHED — по полному времени (wall ± bonus/penalty). DNF внизу.
      </p>
    </div>
  );
}

function StatisticsRow({
  row,
  levels,
}: {
  row: StatisticsTeamRow;
  levels: { levelId: number; orderIndex: number }[];
}) {
  const cellByLevel = new Map(row.levelCells.map((c) => [c.levelId, c]));

  return (
    <tr className="border-b border-border last:border-0">
      <td className="px-3 py-2 tabular-nums text-muted-foreground">
        {row.progressStatus === "DNF" ? "—" : (row.rank ?? "—")}
      </td>
      <td className="px-3 py-2">
        <div className="font-medium">{row.teamName}</div>
        <div className="text-muted-foreground text-xs">
          {PROGRESS_LABEL[row.progressStatus] ?? row.progressStatus}
        </div>
      </td>
      {levels.map((col) => {
        const cell = cellByLevel.get(col.levelId);
        return (
          <td key={col.levelId} className="px-3 py-2 whitespace-nowrap tabular-nums">
            {formatLevelCell(cell?.status ?? null, cell?.completedAt ?? null)}
          </td>
        );
      })}
      <td className="px-3 py-2 tabular-nums">
        {row.bonusPenaltySeconds === 0
          ? "—"
          : `${row.bonusPenaltySeconds > 0 ? "+" : ""}${row.bonusPenaltySeconds}с`}
      </td>
      <td className="px-3 py-2 tabular-nums">
        {row.progressStatus === "DNF"
          ? "DNF"
          : formatDurationSeconds(row.totalTimeSeconds)}
      </td>
    </tr>
  );
}

function formatLevelCell(
  status: string | null,
  completedAt: string | null,
): string {
  if (!status) return "·";
  if (status === "ACTIVE") return "…";
  if (status === "COMPLETED" || status === "AUTO_TRANSITIONED") {
    return completedAt ? formatDateTime(completedAt) : "✓";
  }
  return status;
}
