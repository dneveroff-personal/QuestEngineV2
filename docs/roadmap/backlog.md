# Roadmap / Backlog

Статусы: 🔵 Done · 🟡 In Progress · ⚪ Planned · 🟪 Decision · 💤 Deferred · 🔴 Bug

## Состояние

| Механика | Статус | Комментарий |
|---|---|---|
| Quest CRUD / lifecycle / archive / maximumTeams | 🔵 | |
| Registration + late reg / DNF | 🔵 | |
| Game Mode APIs (current-level, team quests) | 🔵 | |
| Game Mode frontend (hints URL, take, current-level) | 🔵 | PR fix/frontend-game-mode |
| Statistics ranking snapshot | 🔵 | `GET /api/quests/{id}/statistics` |
| Live statistics SSE (ADR-0014) | 🔵 | `GET .../statistics/stream` |
| Auth refresh / rate limit / CI | 🔵 | |
| Удалён unsafe `autoTransitionLevel` | 🔵 | только `tryAutoTransition` (Job 2) |
| `progress.md` / `runtime.md` | 🔵 | runtime = обзор, progress = детали |
| BONUS/PENALTY code one-shot (Scenario 3) | 🔵 | V18 partial UNIQUE + concurrent IT |
| Gameplay WebSocket STOMP (ADR-022) | 🔵 | CODE_*/LEVEL_*/HINT_REVEALED/QUEST_FINISHED; FE socket + live flag; nginx `/ws` |

## Текущие задачи

### Frontend (gaps vs backend)

1. 🔵 **Codes contract** — FE `points` → `bonusPenaltySeconds` (`codes.ts`, `CodesPanel`), как в `CreateCodeRequest` / `CodeResponse`.
2. 🔵 **Страница ranking / statistics** — `/quests/:questId/statistics`: таблица команд (snapshot + SSE при RUNNING); ссылки с detail и Game Mode. «Рейтинг» = ranking из `statistics-ranking.md`, не Elo.
3. 🔵 **Team rename UI** — форма капитана на `/team` (`TeamManagementPanel` + `renameTeam` PATCH).
4. 🔵 **Transfer captain UI** — уже в `TeamMembersList` («Сделать капитаном» + unit tests).
5. 🔵 **Code attempts UI** — team: Game Mode `TeamCodeAttemptsList`; author: Quest detail `AuthorCodeAttemptsList`.
6. 🔵 **Manual time adjustments UI** — Statistics (AUTHOR/ADMIN): create/list/revoke per team progress.
7. 🔵 **My quests history** — `GET /api/teams/my/quests`: текущие + FINISHED.
8. 🔵 **Устаревшие комментарии FE** — MyQuests/Profile/useMyResolvedUser на актуальные API (`/users/me`, team quests).

### Качество (backend — закрыто)

5–11. 🔵 см. историю: BP three sources, CodeSubmission retry, ranking/SSE/CurrentLevel tests, team rename API, code attempt stats API, requiredMainCodesCount, Instant timestamp.

### Realtime gameplay (ADR-022)

- 🔵 **Slice 1–2** — STOMP `/ws`, destination `/topic/quest-progress/{id}/gameplay`, events CODE_*, LEVEL_COMPLETED, QUEST_FINISHED; FE `@stomp/stompjs` + invalidate queries; polling fallback when offline.
- 🔵 **Slice 3** — `HINT_REVEALED` (Job 3 / takeHint) + `LEVEL_AUTO_TRANSITIONED` (Job 2) + nginx `location /ws` upgrade.

### Отложено

- 💤 Unarchive / персональные подсказки.
- 💤 Расширенная аналитика попыток (агрегаты, фильтры) — базовый список API есть.

## Итог

**Backend MVP + quality закрыты.** Frontend gaps vs backend (#1–8) закрыты. Gameplay WebSocket (ADR-022) — полный production-набор events + FE client.
