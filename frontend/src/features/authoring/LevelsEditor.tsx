import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { createLevel, deleteLevel, getLevelsByQuest, updateLevel, type Level } from "@/api/levels";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { CodesPanel } from "@/features/authoring/CodesPanel";
import { HintsPanel } from "@/features/authoring/HintsPanel";
import { LevelForm } from "@/features/authoring/LevelForm";

/**
 * Список уровней квеста. Редактирование — единый экран по «Изменить»:
 * метаданные уровня + коды + подсказки (не разнесено на клик по title).
 */
export function LevelsEditor({ questId }: { questId: number }) {
  const queryClient = useQueryClient();
  const [editingLevelId, setEditingLevelId] = useState<number | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const { data: levels, isLoading } = useQuery({
    queryKey: ["quests", questId, "levels"],
    queryFn: () => getLevelsByQuest(questId),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["quests", questId, "levels"] });
  };

  const createMutation = useMutation({
    mutationFn: (request: Parameters<typeof createLevel>[1]) => createLevel(questId, request),
    onSuccess: (created) => {
      setIsCreating(false);
      invalidate();
      // Сразу открываем полный редактор — коды/подсказки добавляются там.
      setEditingLevelId(created.id);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ levelId, request }: { levelId: number; request: Parameters<typeof updateLevel>[1] }) =>
      updateLevel(levelId, request),
    onSuccess: () => {
      invalidate();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteLevel,
    onSuccess: (_data, levelId) => {
      if (editingLevelId === levelId) setEditingLevelId(null);
      invalidate();
    },
  });

  const deleteError = deleteMutation.error instanceof ApiError ? deleteMutation.error.message : null;

  function handleDelete(level: Level) {
    if (window.confirm(`Удалить уровень "${level.title}"? Подсказки и коды уровня удалятся тоже.`)) {
      deleteMutation.mutate(level.id);
    }
  }

  function levelSummary(level: Level) {
    const parts: string[] = [];
    if (level.timeoutSeconds) parts.push(`автопереход ${level.timeoutSeconds}с`);
    if (level.requiredMainCodesCount) {
      parts.push(`кодов для закрытия: ${level.requiredMainCodesCount}`);
    }
    return parts.length > 0 ? parts.join(" · ") : "без автоперехода и порога кодов";
  }

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка уровней...</p>;
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-medium">Уровни ({levels?.length ?? 0})</h2>
        {!isCreating && (
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setIsCreating(true);
              setEditingLevelId(null);
            }}
          >
            Добавить уровень
          </Button>
        )}
      </div>

      {deleteError && <p className="text-destructive text-sm">{deleteError}</p>}

      {isCreating && (
        <div className="space-y-3 rounded-lg border border-border p-3">
          <p className="text-muted-foreground text-xs">
            Сначала сохраните название и настройки уровня — после этого откроется редактор кодов и
            подсказок.
          </p>
          <LevelForm
            onSubmit={(request) => createMutation.mutate(request)}
            isPending={createMutation.isPending}
            error={createMutation.error}
            submitLabel="Создать уровень"
            onCancel={() => setIsCreating(false)}
          />
        </div>
      )}

      {levels && levels.length === 0 && !isCreating && (
        <p className="text-muted-foreground text-sm">
          Уровней пока нет. Квест нельзя опубликовать без хотя бы одного уровня.
        </p>
      )}

      <ul className="space-y-2">
        {(levels ?? [])
          .slice()
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((level) => (
            <li key={level.id} className="rounded-lg border border-border p-3">
              {editingLevelId === level.id ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-medium">
                      Редактирование: {level.orderIndex}. {level.title}
                    </h3>
                    <Button size="sm" variant="ghost" onClick={() => setEditingLevelId(null)}>
                      Закрыть
                    </Button>
                  </div>

                  <LevelForm
                    level={level}
                    onSubmit={(request) => updateMutation.mutate({ levelId: level.id, request })}
                    isPending={updateMutation.isPending}
                    error={updateMutation.error}
                    submitLabel="Сохранить настройки"
                  />

                  <div className="space-y-4 border-t border-border pt-3">
                    <CodesPanel questId={questId} levelId={level.id} />
                    <HintsPanel questId={questId} levelId={level.id} />
                  </div>
                </div>
              ) : (
                <div className="flex items-center justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-sm font-medium">
                      {level.orderIndex}. {level.title}
                    </p>
                    <p className="text-muted-foreground text-xs">{levelSummary(level)}</p>
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setIsCreating(false);
                        setEditingLevelId(level.id);
                      }}
                    >
                      Изменить
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => handleDelete(level)}>
                      Удалить
                    </Button>
                  </div>
                </div>
              )}
            </li>
          ))}
      </ul>
    </div>
  );
}
