import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { createLevel, deleteLevel, getLevelsByQuest, updateLevel, type Level } from "@/api/levels";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { CodesPanel } from "@/features/authoring/CodesPanel";
import { HintsPanel } from "@/features/authoring/HintsPanel";
import { LevelForm } from "@/features/authoring/LevelForm";

export function LevelsEditor({ questId }: { questId: number }) {
  const queryClient = useQueryClient();
  const [expandedLevelId, setExpandedLevelId] = useState<number | null>(null);
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
    onSuccess: () => {
      setIsCreating(false);
      invalidate();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ levelId, request }: { levelId: number; request: Parameters<typeof updateLevel>[1] }) =>
      updateLevel(levelId, request),
    onSuccess: () => {
      setEditingLevelId(null);
      invalidate();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteLevel,
    onSuccess: invalidate,
  });

  const deleteError = deleteMutation.error instanceof ApiError ? deleteMutation.error.message : null;

  function handleDelete(level: Level) {
    if (window.confirm(`Удалить уровень "${level.title}"? Подсказки и коды уровня удалятся тоже.`)) {
      deleteMutation.mutate(level.id);
    }
  }

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка уровней...</p>;
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-medium">Уровни ({levels?.length ?? 0})</h2>
        {!isCreating && (
          <Button size="sm" variant="outline" onClick={() => setIsCreating(true)}>
            Добавить уровень
          </Button>
        )}
      </div>

      {deleteError && <p className="text-destructive text-sm">{deleteError}</p>}

      {isCreating && (
        <div className="rounded-lg border border-border p-3">
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
                <LevelForm
                  level={level}
                  onSubmit={(request) => updateMutation.mutate({ levelId: level.id, request })}
                  isPending={updateMutation.isPending}
                  error={updateMutation.error}
                  submitLabel="Сохранить"
                  onCancel={() => setEditingLevelId(null)}
                />
              ) : (
                <>
                  <div className="flex items-center justify-between">
                    <button
                      type="button"
                      className="text-left text-sm font-medium"
                      onClick={() =>
                        setExpandedLevelId(expandedLevelId === level.id ? null : level.id)
                      }
                    >
                      {level.orderIndex}. {level.title}
                    </button>
                    <div className="flex gap-1">
                      <Button size="sm" variant="ghost" onClick={() => setEditingLevelId(level.id)}>
                        Изменить
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => handleDelete(level)}>
                        Удалить
                      </Button>
                    </div>
                  </div>

                  {expandedLevelId === level.id && (
                    <div className="mt-3 space-y-4 border-t border-border pt-3">
                      <HintsPanel questId={questId} levelId={level.id} />
                      <CodesPanel questId={questId} levelId={level.id} />
                    </div>
                  )}
                </>
              )}
            </li>
          ))}
      </ul>
    </div>
  );
}
