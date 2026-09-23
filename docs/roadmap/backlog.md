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
9. ⚪ Статистика попыток ввода кода не реализована как API. `statistics-ranking.md` и `code-submission.md` явно описывают видимость: автору — полная статистика попыток всех команд, команде — только своя на активном уровне. В коде нет ни одного эндпоинта, отдающего список/сводку `CodeSubmission` (кроме самого `POST .../codes`) — фича описана в доменной модели, но не существует как API.
10. 🔵 `Level.requiredMainCodesCount` валидируется относительно числа MAIN-кодов (по `codeIndex`) при `updateLevel` и при `validateQuestPublishable` — недостижимый порог отклоняется как Conflict (ADR-0005).
11. 🔵 `GlobalExceptionHandler.timestamp` — `Instant.now()`; расхождение с conventions устранено.

### Отложено

- 💤 WebSocket игрового процесса (ADR-022).
- 💤 Unarchive / персональные подсказки.
- 💤 Детальная статистика попыток кодов автору (см. также п. 9 — теперь это не только "детальная", а вообще отсутствующая базовая версия).

## Итог

**Доменный backlog MVP и критичные frontend-баги Game Mode закрыты.** Quality #5–8, #10–11 закрыты. Остался quality-пункт **#9** (статистика попыток кодов как API).
