import { useNavigate } from "react-router-dom";

import { ApiError } from "@/api/errors";
import type { Quest } from "@/api/quests";
import { Button } from "@/components/ui/button";
import { useQuestLifecycleActions } from "@/features/authoring/useAuthoredQuests";

/**
 * Прекондиции сверены с QuestServiceImpl:
 * - publish: только из DRAFT, квест не должен быть архивным.
 * - finish: только из RUNNING, квест не должен быть архивным.
 * - archive ("Удалить"): backend делает soft-delete (Quest.archived = true), история
 *   регистраций/прохождения не удаляется ни в каком статусе — простого confirm() достаточно
 *   (roadmap/backlog.md, п.16).
 */
export function QuestLifecycleActions({ quest }: { quest: Quest }) {
  const navigate = useNavigate();
  const { publish, finish, remove } = useQuestLifecycleActions(quest.id);

  const error = publish.error ?? finish.error ?? remove.error;
  const errorMessage = error instanceof ApiError ? error.message : null;

  function handleArchiveClick() {
    if (window.confirm(`Архивировать квест "${quest.title}"? История прохождения сохранится.`)) {
      remove.mutate(undefined, { onSuccess: () => navigate("/author", { replace: true }) });
    }
  }

  if (quest.archived) {
    return <p className="text-muted-foreground text-sm">Квест в архиве.</p>;
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
        <Button variant="outline" onClick={handleArchiveClick} disabled={remove.isPending}>
          {remove.isPending ? "Архивируем..." : "Удалить"}
        </Button>
      </div>
    </div>
  );
}
