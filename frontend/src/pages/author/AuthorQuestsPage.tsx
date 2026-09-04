import { Link } from "react-router-dom";

import { buttonVariants } from "@/components/ui/button";
import { formatDateTime } from "@/lib/format";
import { useMyAuthoredQuests } from "@/features/authoring";

const STATUS_LABEL: Record<string, string> = {
  DRAFT: "Черновик",
  REGISTRATION: "Регистрация",
  RUNNING: "Идёт",
  FINISHED: "Завершён",
};

export function AuthorQuestsPage() {
  const { data: quests, isLoading, isError, authorId } = useMyAuthoredQuests();

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Мои квесты (авторство)</h1>
        <Link to="/author/quests/new" className={buttonVariants({ size: "default" })}>
          Создать квест
        </Link>
      </div>

      {authorId === null && !isLoading && (
        <p className="text-destructive text-sm">
          Не удалось определить ваш профиль для показа списка ваших квестов
          (см. docs/roadmap/backlog.md — нет прямого способа узнать свой
          userId). Вы всё ещё можете создать новый квест или открыть
          существующий по прямой ссылке, если знаете его id.
        </p>
      )}

      {isLoading && <p className="text-muted-foreground text-sm">Загрузка...</p>}

      {isError && (
        <p className="text-destructive text-sm">
          Не удалось загрузить список квестов. Попробуйте обновить страницу.
        </p>
      )}

      {quests && quests.length === 0 && (
        <p className="text-muted-foreground text-sm">Вы ещё не создали ни одного квеста.</p>
      )}

      {quests && quests.length > 0 && (
        <ul className="divide-y divide-border rounded-lg border border-border">
          {quests.map((quest) => (
            <li key={quest.id}>
              <Link
                to={`/author/quests/${quest.id}/edit`}
                className="flex items-center justify-between px-4 py-3 text-sm transition-colors hover:bg-accent"
              >
                <span className="font-medium">{quest.title}</span>
                <span className="text-muted-foreground">
                  {STATUS_LABEL[quest.status] ?? quest.status} · {formatDateTime(quest.createdAt)}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
