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

5. 🟡 Runtime Bonus/Penalty — полный integration-путь трёх источников (атомарность BONUS/PENALTY-кода закрыта V18; остаются Hint + Manual).
6. ⚪ Повтор CodeSubmission после потери соединения (контракт).
7. ⚪ Тесты ranking/SSE / 14–16 — по мере необходимости (Odissey). **Уточнение: `StatisticsServiceImpl`/`StatisticsController`/`StatisticsSseHub` не имеют вообще ни одного теста (0 файлов), как и `CurrentLevelServiceImpl`/`CurrentLevelService` (Game Mode агрегирующий эндпоинт) — оба сейчас непроверены совсем, не просто "недостаточно".**
8. ⚪ Нет `TeamController`-эндпоинта переименования команды. `01-domain/team.md` явно описывает это как полномочие капитана ("Переименование", отдельный раздел с примером) — в коде ни `TeamService`, ни `TeamController` такого действия не содержат.
9. ⚪ Статистика попыток ввода кода не реализована как API. `statistics-ranking.md` и `code-submission.md` явно описывают видимость: автору — полная статистика попыток всех команд, команде — только своя на активном уровне. В коде нет ни одного эндпоинта, отдающего список/сводку `CodeSubmission` (кроме самого `POST .../codes`) — фича описана в доменной модели, но не существует как API.
10. 🔵 `Level.requiredMainCodesCount` валидируется относительно числа MAIN-кодов (по `codeIndex`) при `updateLevel` и при `validateQuestPublishable` — недостижимый порог отклоняется как Conflict (ADR-0005).
11. 🔵 `GlobalExceptionHandler.timestamp` — `Instant.now()`; расхождение с conventions устранено.

### Отложено

- 💤 WebSocket игрового процесса (ADR-022).
- 💤 Unarchive / персональные подсказки.
- 💤 Детальная статистика попыток кодов автору (см. также п. 9 — теперь это не только "детальная", а вообще отсутствующая базовая версия).

## Итог

**Доменный backlog MVP и критичные frontend-баги Game Mode закрыты.** Quality #10–11 закрыты. Остались quality-пункты (п. 5–9). Ближайший осмысленный фокус — тесты ranking/SSE/CurrentLevel или rename team / code-attempt stats по продуктовому приоритету.
