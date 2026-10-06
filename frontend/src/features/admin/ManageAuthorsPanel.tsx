import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { setUserRole, searchUsers, type User, type UserRole } from "@/api/users";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const ROLE_LABEL: Record<UserRole, string> = {
  PLAYER: "Игрок",
  AUTHOR: "Автор",
  ADMIN: "Администратор",
};

/** ADMIN: search users; assign/remove AUTHOR via PUT /api/users/{id}/role. */
export function ManageAuthorsPanel() {
  const [username, setUsername] = useState("");
  const [submitted, setSubmitted] = useState("");
  const queryClient = useQueryClient();

  const searchQuery = useQuery({
    queryKey: ["admin", "users", "search", submitted],
    queryFn: () => searchUsers({ username: submitted || undefined, size: 50 }),
    enabled: submitted.length > 0,
  });

  const authorsQuery = useQuery({
    queryKey: ["admin", "users", "authors"],
    queryFn: () => searchUsers({ role: "AUTHOR", size: 50 }),
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: UserRole }) =>
      setUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
  });

  const mutationError =
    roleMutation.error instanceof ApiError
      ? roleMutation.error.message
      : roleMutation.error
        ? "Не удалось изменить роль."
        : null;

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    const q = username.trim();
    if (!q) return;
    setSubmitted(q);
  }

  function renderRow(user: User) {
    const role = user.role;
    const busy = roleMutation.isPending;
    return (
      <li
        key={user.id}
        className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 text-sm"
      >
        <div>
          <p className="font-medium">{user.publicName || user.username}</p>
          <p className="text-muted-foreground text-xs">
            @{user.username} · id {user.id}
            {role ? ` · ${ROLE_LABEL[role] ?? role}` : ""}
          </p>
        </div>
        <div className="flex gap-2">
          {role === "PLAYER" && (
            <Button
              size="sm"
              disabled={busy}
              onClick={() => roleMutation.mutate({ userId: user.id, role: "AUTHOR" })}
            >
              Сделать автором
            </Button>
          )}
          {role === "AUTHOR" && (
            <Button
              size="sm"
              variant="outline"
              disabled={busy}
              onClick={() => roleMutation.mutate({ userId: user.id, role: "PLAYER" })}
            >
              Убрать роль автора
            </Button>
          )}
          {role === "ADMIN" && (
            <span className="text-muted-foreground text-xs">
              Админ — без смены роли здесь
            </span>
          )}
        </div>
      </li>
    );
  }

  return (
    <div className="space-y-6">
      {mutationError && <p className="text-destructive text-sm">{mutationError}</p>}
      {roleMutation.isSuccess && (
        <p className="text-muted-foreground text-sm">
          Роль обновлена. У затронутого пользователя меню обновится после
          повторного входа (роль в JWT).
        </p>
      )}

      <form onSubmit={onSearch} className="space-y-2 rounded-lg border border-border p-4">
        <Label htmlFor="admin-user-search">Поиск по username</Label>
        <div className="flex gap-2">
          <Input
            id="admin-user-search"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="username"
            autoComplete="off"
          />
          <Button type="submit" disabled={!username.trim()}>
            Найти
          </Button>
        </div>
      </form>

      {submitted && (
        <div className="space-y-2">
          <h2 className="text-sm font-medium">Результаты поиска</h2>
          {searchQuery.isLoading && (
            <p className="text-muted-foreground text-sm">Поиск...</p>
          )}
          {searchQuery.isError && (
            <p className="text-destructive text-sm">Не удалось выполнить поиск.</p>
          )}
          {searchQuery.data && searchQuery.data.content.length === 0 && (
            <p className="text-muted-foreground text-sm">Никого не найдено.</p>
          )}
          {searchQuery.data && searchQuery.data.content.length > 0 && (
            <ul className="divide-y divide-border rounded-lg border border-border">
              {searchQuery.data.content.map(renderRow)}
            </ul>
          )}
        </div>
      )}

      <div className="space-y-2">
        <h2 className="text-sm font-medium">Текущие авторы</h2>
        {authorsQuery.isLoading && (
          <p className="text-muted-foreground text-sm">Загрузка...</p>
        )}
        {authorsQuery.isError && (
          <p className="text-destructive text-sm">Не удалось загрузить список авторов.</p>
        )}
        {authorsQuery.data && authorsQuery.data.content.length === 0 && (
          <p className="text-muted-foreground text-sm">Авторов пока нет.</p>
        )}
        {authorsQuery.data && authorsQuery.data.content.length > 0 && (
          <ul className="divide-y divide-border rounded-lg border border-border">
            {authorsQuery.data.content.map(renderRow)}
          </ul>
        )}
      </div>
    </div>
  );
}
