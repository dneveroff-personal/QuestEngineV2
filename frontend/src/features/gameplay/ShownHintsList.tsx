import { formatDateTime } from "@/lib/format";
import { useShownHints } from "@/features/gameplay/useGameplay";

const TYPE_LABEL: Record<string, string> = {
  REGULAR: "Подсказка",
  BONUS: "Бонусная подсказка",
  PENALTY: "Штрафная подсказка",
};

/**
 * Только уже показанные подсказки (auto-reveal по таймеру backend,
 * ADR-0020) — сколько ждать до следующей неизвестно, backend не отдаёт
 * delaySeconds следующей ещё нераскрытой подсказки через игровой API
 * (только через авторский CRUD, которым команда не пользуется).
 */
export function ShownHintsList({ questId, teamId }: { questId: number; teamId: number }) {
  const { data: hints, isLoading } = useShownHints(questId, teamId);

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка подсказок...</p>;
  }

  if (!hints || hints.length === 0) {
    return (
      <div className="rounded-lg border border-border p-4">
        <h2 className="text-sm font-medium">Подсказки</h2>
        <p className="text-muted-foreground text-sm">Пока нет показанных подсказок.</p>
      </div>
    );
  }

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <h2 className="text-sm font-medium">Подсказки ({hints.length})</h2>
      <ul className="space-y-2">
        {hints
          .slice()
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((hint) => (
            <li key={hint.hintId} className="text-sm">
              <p className="text-muted-foreground text-xs">
                {TYPE_LABEL[hint.type] ?? hint.type} · {formatDateTime(hint.shownAt)}
              </p>
              <p>{hint.content}</p>
            </li>
          ))}
      </ul>
    </div>
  );
}
