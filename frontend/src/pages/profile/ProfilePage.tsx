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
 * Профиль: публичное имя, username, роль, дата регистрации.
 * История участия команды — на «Мои квесты» (GET /api/teams/my/quests).
 * Ranking — /quests/:id/statistics. Авторские квесты — блок ниже.
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
            ) : profile?.email ? (
              profile.email
            ) : (
              <span className="text-muted-foreground italic">недоступно</span>
            )}
          </dd>

          <dt className="text-muted-foreground">На платформе с</dt>
          <dd>
            {isProfileLoading ? (
              "Загрузка..."
            ) : profile?.createdAt ? (
              formatDateTime(profile.createdAt)
            ) : (
              <span className="text-muted-foreground italic">недоступно</span>
            )}
          </dd>
        </dl>
        {!isProfileLoading && !profile && (
          <p className="text-muted-foreground text-xs">
            Не удалось загрузить профиль. Обновите страницу или войдите снова.
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
        <h2 className="text-sm font-medium">История участия</h2>
        <p className="text-muted-foreground text-sm">
          Регистрации и завершённые квесты команды — на странице{" "}
          <Link to="/my-quests" className="text-primary underline underline-offset-4">
            Мои квесты
          </Link>
          . Рейтинг во время/после игры — на странице статистики квеста.
        </p>
      </div>
    </div>
  );
}
