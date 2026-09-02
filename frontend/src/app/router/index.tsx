import { createBrowserRouter } from "react-router-dom";

import { RootLayout } from "@/app/layouts/RootLayout";
import { ProtectedRoute } from "@/app/router/ProtectedRoute";
import { LoginPage } from "@/pages/auth/LoginPage";
import { RegisterPage } from "@/pages/auth/RegisterPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ProfilePage } from "@/pages/profile/ProfilePage";
import { HomePage } from "@/pages/quests/HomePage";
import { MyQuestsPage } from "@/pages/quests/MyQuestsPage";
import { QuestDetailPage } from "@/pages/quests/QuestDetailPage";
import { TeamPage } from "@/pages/team/TeamPage";

/**
 * Маршруты соответствуют architecture.md §4.
 *
 * Ещё не добавлены (появятся вместе с соответствующей feature):
 * /author, /author/quests/:questId/edit — авторский раздел;
 * /quests/:questId/play — Game Mode, отдельный layout без RootLayout;
 * /quests/:questId/statistics — зависит от backend statistics/ (не
 * реализован, см. roadmap/backlog.md).
 */
export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/register",
    element: <RegisterPage />,
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <RootLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <HomePage /> },
      { path: "quests/:questId", element: <QuestDetailPage /> },
      { path: "my-quests", element: <MyQuestsPage /> },
      { path: "team", element: <TeamPage /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
