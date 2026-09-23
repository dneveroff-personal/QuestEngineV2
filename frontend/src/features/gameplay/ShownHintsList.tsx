import { formatDateTime } from "@/lib/format";
import { useShownHints, useTakeHint } from "@/features/gameplay/useGameplay";
import type { ShownHint } from "@/api/hints";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/api/errors";

const TYPE_LABEL: Record<string, string> = {
  REGULAR: "Подсказка",
  BONUS: "Бонусная подсказка",
  PENALTY: "Штрафная подсказка",
};

function isAvailableNotTaken(hint: ShownHint): boolean {
  return (
    (hint.type === "BONUS" || hint.type === "PENALTY") &&
    (hint.content == null || hint.content === "") &&
    hint.shownAt == null
  );
}

/**
 * Видимые подсказки уровня:
 * - REGULAR / взятые BONUS|PENALTY — полный текст (auto-reveal Job 3 / take)
 * - BONUS|PENALTY доступны, но не взяты — кнопка «Взять» (ADR-0021)
 */
export function ShownHintsList({ questId, teamId }: { questId: number; teamId: number }) {
  const { data: hints, isLoading } = useShownHints(questId, teamId);
  const takeMutation = useTakeHint(questId, teamId);

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка подсказок...</p>;
  }

  if (!hints || hints.length === 0) {
    return (
      <div className="rounded-lg border border-border p-4">
        <h2 className="text-sm font-medium">Подсказки</h2>
        <p className="text-muted-foreground text-sm">Пока нет доступных подсказок.</p>
      </div>
    );
  }

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <h2 className="text-sm font-medium">Подсказки ({hints.length})</h2>
      {takeMutation.error instanceof ApiError && (
        <p className="text-destructive text-sm">{takeMutation.error.message}</p>
      )}
      <ul className="space-y-2">
        {hints
          .slice()
          .sort((a: ShownHint, b: ShownHint) => a.orderIndex - b.orderIndex)
          .map((hint: ShownHint) => {
            const available = isAvailableNotTaken(hint);
            return (
              <li key={hint.hintId} className="text-sm">
                <p className="text-muted-foreground text-xs">
                  {TYPE_LABEL[hint.type] ?? hint.type}
                  {hint.shownAt ? ` · ${formatDateTime(hint.shownAt)}` : null}
                  {!available && hint.bonusPenaltySeconds != null
                    ? ` · ${hint.type === "BONUS" ? "−" : "+"}${hint.bonusPenaltySeconds} с`
                    : null}
                </p>
                {available ? (
                  <div className="mt-1 flex items-center gap-2">
                    <p className="text-muted-foreground text-xs">
                      Доступна. Взятие раскроет текст
                      {hint.type === "BONUS" ? " и бонус ко времени" : " и штраф ко времени"}.
                    </p>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={takeMutation.isPending}
                      onClick={() => takeMutation.mutate(hint.hintId)}
                    >
                      {takeMutation.isPending ? "..." : "Взять"}
                    </Button>
                  </div>
                ) : (
                  <p>{hint.content}</p>
                )}
              </li>
            );
          })}
      </ul>
    </div>
  );
}
