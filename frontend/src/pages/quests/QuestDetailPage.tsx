import { useParams } from "react-router-dom";

/**
 * Страница отдельного Quest (design-system.md §10).
 *
 * TODO: GET /quests/{questId}, отобразить порядок:
 * Название → Основная информация → Описание → Участники → Действие
 * пользователя (Подать заявку / Заявка отправлена / Войти в игру —
 * в зависимости от registration status).
 */
export function QuestDetailPage() {
  const { questId } = useParams<{ questId: string }>();

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Квест {questId}</h1>
      <p className="text-muted-foreground text-sm">
        Информация о квесте появится здесь.
      </p>
    </div>
  );
}
