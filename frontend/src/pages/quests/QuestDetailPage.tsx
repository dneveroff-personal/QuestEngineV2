import { useParams } from "react-router-dom";

import { useQuest, useQuestRegistrations, RegistrationPanel } from "@/features/quests";
import { formatDateTime } from "@/lib/format";
import { NotFoundPage } from "@/pages/NotFoundPage";

const QUEST_STATUS_LABEL: Record<string, string> = {
  DRAFT: "Черновик",
  REGISTRATION: "Регистрация открыта",
  RUNNING: "Идёт",
  FINISHED: "Завершён",
};

const QUEST_TYPE_LABEL: Record<string, string> = {
  SINGLE: "Одиночный",
  TEAM: "Командный",
};

export function QuestDetailPage() {
  const { questId: questIdParam } = useParams<{ questId: string }>();
  const questId = Number(questIdParam);

  const questQuery = useQuest(questId);
  const registrationsQuery = useQuestRegistrations(questId);

  if (!questIdParam || Number.isNaN(questId)) {
    return <NotFoundPage />;
  }

  if (questQuery.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка...</p>;
  }

  if (questQuery.isError || !questQuery.data) {
    return (
      <p className="text-destructive text-sm">
        Не удалось загрузить квест. Возможно, он был удалён.
      </p>
    );
  }

  const quest = questQuery.data;

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">{quest.title}</h1>
        <div className="text-muted-foreground flex gap-3 text-sm">
          <span>{QUEST_STATUS_LABEL[quest.status] ?? quest.status}</span>
          <span>·</span>
          <span>{QUEST_TYPE_LABEL[quest.type] ?? quest.type}</span>
          <span>·</span>
          <span>{formatDateTime(quest.startTime)}</span>
        </div>
      </div>

      {quest.description && <p className="whitespace-pre-wrap text-sm">{quest.description}</p>}

      {registrationsQuery.data && (
        <RegistrationPanel quest={quest} registrations={registrationsQuery.data} />
      )}
    </div>
  );
}
