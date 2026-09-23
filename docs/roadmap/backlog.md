# Roadmap / Backlog

Статусы: 🔵 Done · 🟡 In Progress · ⚪ Planned · 🟣 Decision · 💤 Deferred · 🔴 Bug

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

## Текущие задачи

### Frontend (gaps vs backend)

1. 🔵 **Codes contract** — FE `points` → `bonusPenaltySeconds` (`codes.ts`, `CodesPanel`), как в `CreateCodeRequest` / `CodeResponse`.
2. 🔵 **Страница ranking / statistics** — `/quests/:questId/statistics`: таблица команд (snapshot + SSE при RUNNING); ссылки с detail и Game Mode. «Рейтинг» = ranking из `statistics-ranking.md`, не Elo.
3. ⚪ **Team rename UI** — backend `PATCH /api/teams/{teamId}` есть; FE нет формы для капитана.
4. ⚪ **Transfer captain UI** — backend `POST .../transfer-captain` есть; FE нет.
5. ⚪ **Code attempts UI** — backend GET team ACTIVE + author full; FE клиент/экран нет.
6. ⚪ **Manual time adjustments UI** — backend create/list/revoke; FE нет.
7. ⚪ **My quests history** — показывать FINISHED / историю, не только upcoming; backend team/my quests шире UI.
8. ⚪ **Устаревшие комментарии FE** — router/MyQuests/Profile про «backend не готов» (частично снято вместе с #1–2).

### Качество (backend — закрыто)

5–11. 🔵 см. историю: BP three sources, CodeSubmission retry, ranking/SSE/CurrentLevel tests, team rename API, code attempt stats API, requiredMainCodesCount, Instant timestamp.

### Отложено

- 💤 WebSocket игрового процесса (ADR-022).
- 💤 Unarchive / персональные подсказки.
- 💤 Расширенная аналитика попыток (агрегаты, фильтры) — базовый список API есть.

## Итог

**Backend MVP + quality закрыты.** Frontend: codes-контракт и экран ranking закрыты; остаются team rename/transfer, attempts UI, manual BP UI, history my-quests.
