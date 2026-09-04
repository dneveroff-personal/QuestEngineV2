import { useNavigate } from "react-router-dom";

import { QuestForm, useCreateQuest } from "@/features/authoring";

export function CreateQuestPage() {
  const navigate = useNavigate();
  const mutation = useCreateQuest();

  return (
    <div className="max-w-lg space-y-4">
      <h1 className="text-2xl font-semibold">Новый квест</h1>
      <QuestForm
        onSubmit={(request) =>
          mutation.mutate(request, {
            onSuccess: (quest) => navigate(`/author/quests/${quest.id}/edit`, { replace: true }),
          })
        }
        isPending={mutation.isPending}
        error={mutation.error}
        submitLabel="Создать"
      />
    </div>
  );
}
