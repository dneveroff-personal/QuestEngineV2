import { useNavigate } from "react-router-dom";

import { ApiError } from "@/api/errors";
import type { Quest } from "@/api/quests";
import { Button } from "@/components/ui/button";
import { useQuestLifecycleActions } from "@/features/authoring/useAuthoredQuests";

/**
 * Прекондиции сверены с QuestServiceImpl:
 * - publish: только из DRAFT (плюс backend сам проверит, что есть уровни
 *   и у каждого уровня есть коды или автопереход — ошибку покажем как есть,
 *   не дублируем эту проверку на клиенте, чтобы не разойтись с backend).
 * - finish: только из RUNNING.
 * - delete: разрешён в ЛЮБОМ статусе (backend не проверяет) — поэтому
 *   подтверждение обязательно, особенно для RUNNING/FINISHED.
 */
export function QuestLifecycleActions({ quest }: { quest: Quest }) {
  const navigate = useNavigate();
  const { publish, finish, remove } = useQuestLifecycleActions(quest.id);

  const error = publish.error ?? finish.error ?? remove.error;
  const errorMessage = error instanceof ApiError ? error.message : null;

  function handleDelete() {
    const warning =
      quest.status === "RUNNING" || quest.status === "FINISHED"
        ? `Квест в статусе "${quest.status}" — удаление сотрёт историю прохождения. Точно удалить "${quest.title}"?`
        : `Удалить квест "${quest.title}"? Это необратимо.`;

    if (window.confirm(warning)) {
      remove.mutate(undefined, { onSuccess: () => navigate("/author", { replace: true }) });
    }
  }

  return (
    <div className="space-y-2">
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}
      <div className="flex gap-2">
        {quest.status === "DRAFT" && (
          <Button onClick={() => publish.mutate()} disabled={publish.isPending}>
            {publish.isPending ? "Публикуем..." : "Опубликовать"}
          </Button>
        )}
        {quest.status === "RUNNING" && (
          <Button onClick={() => finish.mutate()} disabled={finish.isPending}>
            {finish.isPending ? "Завершаем..." : "Завершить"}
          </Button>
        )}
        <Button variant="outline" onClick={handleDelete} disabled={remove.isPending}>
          {remove.isPending ? "Удаляем..." : "Удалить"}
        </Button>
      </div>
    </div>
  );
}
