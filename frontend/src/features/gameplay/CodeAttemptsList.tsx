import type { CodeSubmissionAttempt, CodeSubmissionResult } from "@/api/gameplay";
import { useAuthorAttempts, useTeamAttempts } from "@/features/gameplay/useGameplay";
import { formatDateTime } from "@/lib/format";

const RESULT_LABEL: Record<CodeSubmissionResult, { text: string; className: string }> = {
  CORRECT_MAIN: { text: "MAIN", className: "text-success" },
  CORRECT_BONUS: { text: "BONUS", className: "text-success" },
  CORRECT_PENALTY: { text: "PENALTY", className: "text-warning" },
  INCORRECT: { text: "неверно", className: "text-destructive" },
};

function AttemptsTable({
  attempts,
  showTeam,
  showLevel,
}: {
  attempts: CodeSubmissionAttempt[];
  showTeam?: boolean;
  showLevel?: boolean;
}) {
  if (attempts.length === 0) {
    return <p className="text-muted-foreground text-sm">Попыток пока нет.</p>;
  }

  // Newest first for readability during live play / audit.
  const sorted = [...attempts].sort(
    (a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt),
  );

  return (
    <ul className="divide-y divide-border rounded-lg border border-border text-sm">
      {sorted.map((a) => {
        const result = RESULT_LABEL[a.result] ?? {
          text: a.result,
          className: "text-muted-foreground",
        };
        return (
          <li key={a.id} className="flex flex-wrap items-baseline gap-x-3 gap-y-1 px-3 py-2">
            <span className="font-mono">{a.rawValue}</span>
            <span className={result.className}>{result.text}</span>
            {showTeam && (
              <span className="text-muted-foreground truncate" title={a.teamName}>
                {a.teamName}
              </span>
            )}
            {showLevel && (
              <span className="text-muted-foreground">ур. {a.levelOrderIndex}</span>
            )}
            <span className="text-muted-foreground ml-auto shrink-0">
              {a.submittedByUsername} · {formatDateTime(a.submittedAt)}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

/** Team Game Mode: attempts on the currently ACTIVE level only. */
export function TeamCodeAttemptsList({
  questId,
  teamId,
  enabled = true,
  live = false,
}: {
  questId: number;
  teamId: number;
  enabled?: boolean;
  live?: boolean;
}) {
  const query = useTeamAttempts(questId, teamId, enabled, live);

  if (query.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка попыток...</p>;
  }

  if (query.isError) {
    return (
      <p className="text-destructive text-sm">
        Не удалось загрузить попытки.
      </p>
    );
  }

  return (
    <div className="space-y-2">
      <h2 className="text-sm font-medium">Попытки на уровне</h2>
      <AttemptsTable attempts={query.data ?? []} />
    </div>
  );
}

/** Author/ADMIN: full audit across teams and levels. */
export function AuthorCodeAttemptsList({
  questId,
  enabled = true,
}: {
  questId: number;
  enabled?: boolean;
}) {
  const query = useAuthorAttempts(questId, enabled);

  if (!enabled) return null;

  if (query.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка попыток...</p>;
  }

  if (query.isError) {
    return (
      <p className="text-destructive text-sm">
        Не удалось загрузить статистику попыток.
      </p>
    );
  }

  return (
    <div className="space-y-2">
      <h2 className="text-sm font-medium">Все попытки ввода кодов</h2>
      <AttemptsTable attempts={query.data ?? []} showTeam showLevel />
    </div>
  );
}
