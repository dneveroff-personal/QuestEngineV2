import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { ApiError } from "@/api/errors";
import type { Quest } from "@/api/quests";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useQuestLifecycleActions } from "@/features/authoring/useAuthoredQuests";

/**
 * Прекондиции сверены с QuestServiceImpl:
 * - publish: только из DRAFT (плюс backend сам проверит, что есть уровни
 *   и у каждого уровня есть коды или автопереход — ошибку покажем как есть,
 *   не дублируем эту проверку на клиенте, чтобы не разойтись с backend).
 * - finish: только из RUNNING.
 * - delete: разрешён в ЛЮБОМ статусе, backend НЕ проверяет статус вообще
 *   (roadmap/backlog.md, "delete(questId) не проверяет статус квеста" —
 *   всё ещё открыто на 0.6.14). Для DRAFT/REGISTRATION (нет истории
 *   прохождения) — обычный confirm() достаточен. Для RUNNING/FINISHED
 *   (реальная история прохождения на кону, а backend её никак не
 *   защищает) — простого confirm() мало, он слишком легко проходит по
 *   случайному второму клику. Требуем набрать название квеста, как в
 *   GitHub при удалении репозитория — это чисто фронтовая защита,
 *   компенсирующая то, что backend не проверяет.
 */
export function QuestLifecycleActions({ quest }: { quest: Quest }) {
  const navigate = useNavigate();
  const { publish, finish, remove } = useQuestLifecycleActions(quest.id);
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [confirmText, setConfirmText] = useState("");

  const error = publish.error ?? finish.error ?? remove.error;
  const errorMessage = error instanceof ApiError ? error.message : null;

  const isHighRisk = quest.status === "RUNNING" || quest.status === "FINISHED";

  function performDelete() {
    remove.mutate(undefined, { onSuccess: () => navigate("/author", { replace: true }) });
  }

  function handleDeleteClick() {
    if (!isHighRisk) {
      if (window.confirm(`Удалить квест "${quest.title}"? Это необратимо.`)) {
        performDelete();
      }
      return;
    }
    setIsConfirmingDelete(true);
  }

  if (isConfirmingDelete) {
    const canConfirm = confirmText === quest.title;
    return (
      <div className="space-y-2 rounded-lg border border-destructive/50 p-3">
        {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}
        <p className="text-destructive text-sm">
          Квест в статусе "{quest.status}" — у него есть история прохождения команд, которая
          удалится безвозвратно. Backend это не проверяет и не защищает — только это подтверждение.
        </p>
        <p className="text-sm">
          Чтобы удалить, наберите название квеста: <span className="font-medium">{quest.title}</span>
        </p>
        <Input
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          autoFocus
          autoComplete="off"
        />
        <div className="flex gap-2">
          <Button
            variant="outline"
            className="border-destructive text-destructive hover:bg-destructive/10"
            disabled={!canConfirm || remove.isPending}
            onClick={performDelete}
          >
            {remove.isPending ? "Удаляем..." : "Удалить безвозвратно"}
          </Button>
          <Button
            variant="ghost"
            onClick={() => {
              setIsConfirmingDelete(false);
              setConfirmText("");
            }}
          >
            Отмена
          </Button>
        </div>
      </div>
    );
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
        <Button variant="outline" onClick={handleDeleteClick} disabled={remove.isPending}>
          {remove.isPending ? "Удаляем..." : "Удалить"}
        </Button>
      </div>
    </div>
  );
}
