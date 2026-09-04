import { useState } from "react";
import { useParams } from "react-router-dom";

import { useQuest } from "@/features/quests";
import {
  LevelsEditor,
  QuestForm,
  QuestLifecycleActions,
  RegistrationReviewPanel,
  useUpdateQuest,
} from "@/features/authoring";
import { NotFoundPage } from "@/pages/NotFoundPage";

/**
 * Backend НЕ говорит frontend, автор ли текущий пользователь именно
 * ЭТОГО квеста (QuestResponse не отдаёт authorId — см.
 * docs/roadmap/backlog.md). Поэтому редактирование доступно тут любому
 * с ролью AUTHOR/ADMIN (гейт по роли — на роутере/навигации), а
 * фактическую проверку "ваш ли это квест" делает backend через 403 —
 * ошибку показываем как есть, не пытаемся угадать заранее.
 */
export function EditQuestPage() {
  const { questId: questIdParam } = useParams<{ questId: string }>();
  const questId = Number(questIdParam);
  const [isEditingMeta, setIsEditingMeta] = useState(false);

  const questQuery = useQuest(questId);
  const updateMutation = useUpdateQuest(questId);

  if (!questIdParam || Number.isNaN(questId)) {
    return <NotFoundPage />;
  }

  if (questQuery.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка...</p>;
  }

  if (questQuery.isError || !questQuery.data) {
    return <p className="text-destructive text-sm">Не удалось загрузить квест.</p>;
  }

  const quest = questQuery.data;

  return (
    <div className="max-w-2xl space-y-8">
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">{quest.title}</h1>
          {!isEditingMeta && (
            <button
              type="button"
              className="text-primary text-sm underline underline-offset-4"
              onClick={() => setIsEditingMeta(true)}
            >
              Изменить название/описание
            </button>
          )}
        </div>

        {isEditingMeta ? (
          <QuestForm
            quest={quest}
            onSubmit={(request) =>
              updateMutation.mutate(request, { onSuccess: () => setIsEditingMeta(false) })
            }
            isPending={updateMutation.isPending}
            error={updateMutation.error}
            submitLabel="Сохранить"
          />
        ) : (
          <p className="whitespace-pre-wrap text-sm">{quest.description}</p>
        )}

        <QuestLifecycleActions quest={quest} />
      </div>

      <LevelsEditor questId={questId} />

      {(quest.status === "REGISTRATION" || quest.status === "RUNNING") && (
        <RegistrationReviewPanel questId={questId} />
      )}
    </div>
  );
}
