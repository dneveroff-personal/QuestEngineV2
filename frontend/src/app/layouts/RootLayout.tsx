import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth";
import { cn } from "@/lib/utils";

/**
 * Layout для Normal Mode (architecture.md §16).
 *
 * Game Mode (`/quests/:questId/play`) использует отдельный layout без этой
 * навигации — см. src/app/layouts/GameLayout.tsx (появится вместе с
 * игровым экраном).
 */
const NAV_ITEMS = [
  { to: "/my-quests", label: "Мои квесты" },
  { to: "/team", label: "Команда" },
  { to: "/profile", label: "Профиль" },
];

export function RootLayout() {
  const { publicName, role, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    // TODO(ADR-0015): когда появится POST /api/auth/logout — вызвать его
    // здесь (отзыв refresh-токена на backend), прежде чем очищать локальную
    // сессию. Сейчас backend не хранит состояние сессии — logout только
    // локальный.
    logout();
    navigate("/login", { replace: true });
  }

  // role — из JWT (см. lib/jwt.ts), только для UI. Реальная проверка —
  // всегда на backend (validateAuthorOrAdmin), это не граница безопасности.
  const navItems =
    role === "AUTHOR" || role === "ADMIN"
      ? [...NAV_ITEMS, { to: "/author", label: "Авторская" }]
      : NAV_ITEMS;

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-border">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
          <NavLink to="/" className="text-sm font-semibold">
            QuestEngine
          </NavLink>
          <nav className="flex items-center gap-4">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    "text-sm text-muted-foreground transition-colors hover:text-foreground",
                    isActive && "text-foreground font-medium",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
            <span className="text-border">|</span>
            <span className="text-sm text-muted-foreground">{publicName}</span>
            <Button variant="ghost" size="sm" onClick={handleLogout}>
              Выйти
            </Button>
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
