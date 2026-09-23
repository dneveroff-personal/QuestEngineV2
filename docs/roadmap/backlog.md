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

### Качество (не блокеры)

5. 🔵 Runtime Bonus/Penalty — агрегат Code + Hint + Manual: unit (три источника) + IT `bonusPenaltySeconds_aggregatesAllThreeSources`; one-shot кода — V18.
6. 🔵 Повтор CodeSubmission после потери соединения — контракт в `code-submission.md`; IT: retry MAIN на ACTIVE (без double-complete) и 409 после levelCompleted.
7. 🔵 Тесты ranking/SSE / CurrentLevel — unit-тесты `StatisticsServiceImpl` (runtime/final ranking, hide 0 completed, AUTO_TRANSITIONED), `StatisticsSseHub` (subscribe/publish), `CurrentLevelServiceImpl` (member/admin/forbidden/not-found).
8. 🔵 Переименование команды — `PATCH /api/teams/{teamId}` (капитан, уникальность имени, identity по `Team.id`).
9. 🔵 Статистика попыток ввода кода как API: `GET .../progress/{questId}/{teamId}/codes` (команда, ACTIVE) и `GET /api/quests/{questId}/code-submissions` (автор). Контракт в `code-submission.md`.
10. 🔵 `Level.requiredMainCodesCount` валидируется относительно числа MAIN-кодов (по `codeIndex`) при `updateLevel` и при `validateQuestPublishable` — недостижимый порог отклоняется как Conflict (ADR-0005).
11. 🔵 `GlobalExceptionHandler.timestamp` — `Instant.now()`; расхождение с conventions устранено.

### Отложено

- 💤 WebSocket игрового процесса (ADR-022).
- 💤 Unarchive / персональные подсказки.
- 💤 Расширенная аналитика попыток (агрегаты, фильтры по уровню/команде) — базовая выдача списка уже есть (п. 9).

## Итог

**Доменный backlog MVP и критичные frontend-баги Game Mode закрыты.** Quality #5–11 закрыты.
