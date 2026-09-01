import { NavLink, Outlet } from "react-router-dom";

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
  // AUTHOR-пункт добавляется условно, по роли пользователя —
  // architecture.md §14, Authorization и Role-based UI.
];

export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-border">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
          <NavLink to="/" className="text-sm font-semibold">
            QuestEngine
          </NavLink>
          <nav className="flex items-center gap-4">
            {NAV_ITEMS.map((item) => (
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
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
