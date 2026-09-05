import { Link } from "react-router-dom";

import { useAuth, useMyResolvedUser } from "@/features/auth";
import { useMyAuthoredQuests } from "@/features/authoring";
import { formatDateTime } from "@/lib/format";

const ROLE_LABEL: Record<string, string> = {
  PLAYER: "Игрок",
  AUTHOR: "Автор",
  ADMIN: "Администратор",
};

/**
 * Профиль игрока (screens.md §5). Полная спецификация экрана шире, чем
 * реализовано здесь — "история участия", "результаты", "статистика"
 * упираются в полностью не реализованный Statistics (roadmap.md §4.1) и
 * в отсутствие endpoint'а истории завершённых квестов (тот же пробел,
 * что и в MyQuestsPage.tsx §4.4 — /upcoming не отдаёт FINISHED квесты).
 * Личная информация и авторские квесты — реализованы полностью.
 */
export function ProfilePage() {
  const { publicName, username, role } = useAuth();
  const { data: profile, isLoading: isProfileLoading } = useMyResolvedUser();
  const isAuthor = role === "AUTHOR" || role === "ADMIN";
  const authoredQuestsQuery = useMyAuthoredQuests();

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="text-2xl font-semibold">Профиль</h1>

      <div className="space-y-2 rounded-lg border border-border p-4">
        <div className="flex items-center justify-between">
          <span className="text-lg font-medium">{publicName}</span>
          {role && (
            <span className="text-muted-foreground text-xs">
              {ROLE_LABEL[role] ?? role}
            </span>
          )}
        </div>
        <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm">
          <dt className="text-muted-foreground">Username</dt>
          <dd>{username}</dd>

          <dt className="text-muted-foreground">Email</dt>
          <dd>
            {isProfileLoading ? (
              "Загрузка..."
            ) : profile ? (
              profile.email
            ) : (
              <span className="text-muted-foreground italic">недоступно</span>
            )}
          </dd>

          <dt className="text-muted-foreground">На платформе с</dt>
          <dd>
            {isProfileLoading ? (
              "Загрузка..."
            ) : profile ? (
              formatDateTime(profile.createdAt)
            ) : (
              <span className="text-muted-foreground italic">недоступно</span>
            )}
          </dd>
        </dl>
        {!isProfileLoading && !profile && (
          <p className="text-muted-foreground text-xs">
            Email и дата регистрации недоступны напрямую — backend не
            предоставляет способ получить полный профиль текущего
            пользователя (нет <code>GET /api/users/me</code>, см.
            docs/roadmap/backlog.md).
          </p>
        )}
      </div>

      {isAuthor && (
        <div className="space-y-2 rounded-lg border border-border p-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-medium">Авторские квесты</h2>
            <Link to="/author" className="text-primary text-sm underline underline-offset-4">
              Открыть
            </Link>
          </div>
          {authoredQuestsQuery.isLoading && (
            <p className="text-muted-foreground text-sm">Загрузка...</p>
          )}
          {authoredQuestsQuery.data && (
            <p className="text-muted-foreground text-sm">
              Создано квестов: {authoredQuestsQuery.data.length}
            </p>
          )}
        </div>
      )}

      <div className="rounded-lg border border-border p-4">
        <h2 className="text-sm font-medium">История и статистика</h2>
        <p className="text-muted-foreground text-sm">
          Пока недоступно — раздел статистики не реализован на backend
          (docs/frontend/roadmap.md §4.1), а история завершённых квестов
          требует эндпоинта, которого пока тоже нет (§4.4).
        </p>
      </div>
    </div>
  );
}
